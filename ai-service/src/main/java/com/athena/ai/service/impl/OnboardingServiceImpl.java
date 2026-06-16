package com.athena.ai.service.impl;

import com.athena.ai.client.AiException;
import com.athena.ai.client.AiTemporarilyUnavailableException;
import com.athena.ai.constants.AiConstants;
import com.athena.ai.domain.OnboardingStatus;
import com.athena.ai.dto.AssessmentAnswer;
import com.athena.ai.dto.AssessmentResponse;
import com.athena.ai.dto.DailyPlanResponse;
import com.athena.ai.dto.OnboardingResultResponse;
import com.athena.ai.dto.OnboardingStateResponse;
import com.athena.ai.dto.RoadmapResponse;
import com.athena.ai.dto.StartOnboardingResponse;
import com.athena.ai.dto.SubmitAssessmentRequest;
import com.athena.ai.dto.SubmitGoalRequest;
import com.athena.ai.entity.Assessment;
import com.athena.ai.entity.GeneratedDailyPlan;
import com.athena.ai.entity.GeneratedRoadmap;
import com.athena.ai.entity.OnboardingSession;
import com.athena.ai.messaging.AiEventPublisher;
import com.athena.ai.model.AssessmentQuestions;
import com.athena.ai.model.DailyPlanContent;
import com.athena.ai.model.GoalAnalysis;
import com.athena.ai.model.RoadmapContent;
import com.athena.ai.repository.AssessmentRepository;
import com.athena.ai.repository.GeneratedDailyPlanRepository;
import com.athena.ai.repository.GeneratedRoadmapRepository;
import com.athena.ai.repository.OnboardingSessionRepository;
import com.athena.ai.service.AiGenerationService;
import com.athena.ai.service.AiRetryService;
import com.athena.ai.service.KnowledgeGraphService;
import com.athena.ai.service.OnboardingService;
import com.athena.common.event.AssessmentCompletedEvent;
import com.athena.common.event.DailyPlanGeneratedEvent;
import com.athena.common.event.GoalAnalyzedEvent;
import com.athena.common.event.GoalDiscoveredEvent;
import com.athena.common.event.KafkaTopics;
import com.athena.common.event.RoadmapGeneratedEvent;
import com.athena.common.event.UserOnboardingStartedEvent;
import com.athena.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingService {

    private final OnboardingSessionRepository sessionRepository;
    private final AssessmentRepository assessmentRepository;
    private final GeneratedRoadmapRepository roadmapRepository;
    private final GeneratedDailyPlanRepository dailyPlanRepository;
    private final AiGenerationService generation;
    private final AiEventPublisher events;
    private final KnowledgeGraphService knowledgeGraph;
    private final AiRetryService retryService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    @Transactional
    public OnboardingSession createSessionFromRegistration(UUID userId, String firstName, String lastName) {
        return sessionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseGet(() -> {
                    OnboardingSession session = sessionRepository.save(
                            new OnboardingSession(userId, firstName, lastName));
                    events.publish(KafkaTopics.ONBOARDING_STARTED, userId,
                            new UserOnboardingStartedEvent(userId, session.getId(), Instant.now(clock)));
                    log.info("Auto-created onboarding session userId={} sessionId={}", userId, session.getId());
                    return session;
                });
    }

    @Override
    @Deprecated
    @Transactional
    public StartOnboardingResponse start(UUID userId) {
        OnboardingSession session = createSessionFromRegistration(userId, null, null);
        return new StartOnboardingResponse(session.getId(), buildGreeting(session.getFirstName()),
                "What would you like to learn?");
    }

    @Override
    @Transactional
    public AssessmentResponse submitGoal(UUID userId, SubmitGoalRequest request) {
        OnboardingSession session = currentSession(userId);
        session.setGoalText(request.goal());
        session.setStatus(OnboardingStatus.GOAL_SET);
        sessionRepository.save(session);
        events.publish(KafkaTopics.GOAL_DISCOVERED, userId,
                new GoalDiscoveredEvent(userId, session.getId(), request.goal(), Instant.now(clock)));

        try {
            return generateAssessmentForSession(session);
        } catch (AiException ex) {
            throw scheduleRetry(AiConstants.RETRY_ASSESSMENT, userId, session.getId());
        }
    }

    @Override
    @Transactional
    public OnboardingResultResponse submitAssessment(UUID userId, SubmitAssessmentRequest request) {
        OnboardingSession session = currentSession(userId);
        if (session.getGoalText() == null) {
            throw new IllegalArgumentException("Goal must be submitted before the assessment");
        }
        assessmentRepository.findFirstBySessionIdOrderByCreatedAtDesc(session.getId()).ifPresent(a -> {
            a.setAnswersJson(writeJson(request.answers()));
            assessmentRepository.save(a);
        });
        events.publish(KafkaTopics.ASSESSMENT_COMPLETED, userId,
                new AssessmentCompletedEvent(userId, session.getId(), null, Instant.now(clock)));

        try {
            return completeOnboardingForSession(session, request.answers());
        } catch (AiException ex) {
            throw scheduleRetry(AiConstants.RETRY_COMPLETE, userId, session.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingStateResponse getState(UUID userId) {
        OnboardingSession s = currentSession(userId);
        return new OnboardingStateResponse(s.getId(), s.getStatus().name(), buildGreeting(s.getFirstName()),
                s.getGoalText(), s.getDomain(), s.getLevel(), s.getEstimatedMonths(), s.getDailyHours());
    }

    @Override
    @Transactional
    public AssessmentResponse generateAssessmentForSession(OnboardingSession session) {
        AssessmentQuestions questions = generation.generateAssessment(session.getUserId(), session.getGoalText());
        Assessment assessment = assessmentRepository.findFirstBySessionIdOrderByCreatedAtDesc(session.getId())
                .orElseGet(() -> new Assessment(session.getId(), session.getUserId(), null));
        assessment.setQuestionsJson(writeJson(questions.questions()));
        assessmentRepository.save(assessment);
        return new AssessmentResponse(session.getId(), questions.questions());
    }

    @Override
    @Transactional
    public OnboardingResultResponse completeOnboardingForSession(OnboardingSession session,
                                                                 List<AssessmentAnswer> answers) {
        UUID userId = session.getUserId();
        String goal = session.getGoalText();

        GoalAnalysis analysis = generation.analyzeGoal(userId, goal, formatAnswers(answers));
        applyAnalysis(session, analysis);
        events.publish(KafkaTopics.GOAL_ANALYZED, userId, new GoalAnalyzedEvent(
                userId, session.getId(), analysis.domain(), analysis.level(), analysis.estimatedMonths(),
                Instant.now(clock)));

        RoadmapContent roadmapContent = generation.generateRoadmap(userId, goal, analysis);
        GeneratedRoadmap roadmap = roadmapRepository.save(new GeneratedRoadmap(
                userId, session.getId(), goal, analysis.level(), writeJson(roadmapContent)));
        events.publish(KafkaTopics.ROADMAP_GENERATED, userId,
                new RoadmapGeneratedEvent(userId, roadmap.getId(), Instant.now(clock)));

        DailyPlanResponse dailyPlanResponse = null;
        try {
            knowledgeGraph.seedSkills(userId, analysis.domain(), analysis.prerequisites());

            String firstPhase = roadmapContent.phases().isEmpty() ? "Foundations"
                    : roadmapContent.phases().getFirst().name();
            DailyPlanContent planContent = generation.generateDailyPlan(
                    userId, goal, analysis.level(), firstPhase, analysis.dailyHours());
            LocalDate today = LocalDate.now(clock);
            GeneratedDailyPlan dailyPlan = dailyPlanRepository.save(new GeneratedDailyPlan(
                    userId, roadmap.getId(), today, writeJson(planContent)));
            events.publish(KafkaTopics.DAILY_PLAN_GENERATED, userId,
                    new DailyPlanGeneratedEvent(userId, dailyPlan.getId(), today, Instant.now(clock)));
            dailyPlanResponse = new DailyPlanResponse(
                    dailyPlan.getId(), today, planContent.items(), dailyPlan.getCreatedAt());
        } catch (AiException ex) {
            log.warn("Daily plan / KG seeding failed after roadmap was saved; continuing. userId={} cause={}",
                    userId, ex.getClass().getSimpleName());
        }

        session.setStatus(OnboardingStatus.COMPLETED);
        sessionRepository.save(session);
        log.info("Onboarding completed userId={} domain={} level={}", userId, analysis.domain(), analysis.level());

        return new OnboardingResultResponse(
                analysis,
                new RoadmapResponse(roadmap.getId(), roadmap.getGoal(), roadmap.getLevel(),
                        roadmapContent.phases(), roadmap.getCreatedAt()),
                dailyPlanResponse);
    }

    private AiTemporarilyUnavailableException scheduleRetry(String type, UUID userId, UUID sessionId) {
        UUID retryId = retryService.record(type, userId, sessionId.toString()).getId();
        log.warn("AI unavailable; scheduled retry={} type={} userId={}", retryId, type, userId);
        return new AiTemporarilyUnavailableException(retryId);
    }

    private void applyAnalysis(OnboardingSession session, GoalAnalysis analysis) {
        session.setDomain(analysis.domain());
        session.setLevel(analysis.level());
        session.setEstimatedMonths(analysis.estimatedMonths());
        session.setDailyHours(analysis.dailyHours());
        session.setStatus(OnboardingStatus.ASSESSED);
        sessionRepository.save(session);
    }

    private OnboardingSession currentSession(UUID userId) {
        return sessionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> ResourceNotFoundException.of(AiConstants.RESOURCE_ONBOARDING, userId));
    }

    private String buildGreeting(String firstName) {
        String name = firstName == null || firstName.isBlank() ? "there" : firstName;
        return """
                Hello, %s 👋

                I'm Athena. I'll help you build your personal learning journey.

                What would you like to learn?""".formatted(name);
    }

    private String formatAnswers(List<AssessmentAnswer> answers) {
        StringBuilder sb = new StringBuilder();
        for (AssessmentAnswer a : answers) {
            sb.append("- ").append(a.question()).append(" -> ").append(a.answer()).append('\n');
        }
        return sb.toString();
    }

    private String writeJson(Object value) {
        return objectMapper.writeValueAsString(value);
    }
}
