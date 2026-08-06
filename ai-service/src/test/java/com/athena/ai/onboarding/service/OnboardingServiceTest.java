package com.athena.ai.onboarding.service;

import com.athena.ai.llm.AiException;
import com.athena.ai.llm.AiTemporarilyUnavailableException;
import com.athena.ai.generation.service.AiGenerationService;
import com.athena.ai.generation.service.AiRetryService;
import com.athena.ai.knowledgegraph.service.KnowledgeGraphService;
import com.athena.ai.onboarding.service.OnboardingService;
import com.athena.ai.onboarding.service.impl.OnboardingServiceImpl;
import com.athena.ai.onboarding.dto.AssessmentResponse;
import com.athena.ai.onboarding.dto.SubmitGoalRequest;
import com.athena.ai.generation.entity.AiRequestRetry;
import com.athena.ai.onboarding.entity.OnboardingSession;
import com.athena.ai.generation.messaging.AiEventPublisher;
import com.athena.ai.generation.model.AssessmentQuestions;
import com.athena.ai.onboarding.repository.AssessmentRepository;
import com.athena.ai.dailyplan.repository.GeneratedDailyPlanRepository;
import com.athena.ai.roadmap.repository.GeneratedRoadmapRepository;
import com.athena.ai.onboarding.repository.OnboardingSessionRepository;
import com.athena.common.event.KafkaTopics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock private OnboardingSessionRepository sessionRepository;
    @Mock private AssessmentRepository assessmentRepository;
    @Mock private GeneratedRoadmapRepository roadmapRepository;
    @Mock private GeneratedDailyPlanRepository dailyPlanRepository;
    @Mock private AiGenerationService generation;
    @Mock private AiEventPublisher events;
    @Mock private KnowledgeGraphService knowledgeGraph;
    @Mock private AiRetryService retryService;

    private OnboardingService service;

    private final UUID userId = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-13T10:00:00Z"), ZoneOffset.UTC);
        service = new OnboardingServiceImpl(sessionRepository, assessmentRepository, roadmapRepository,
                dailyPlanRepository, generation, events, knowledgeGraph, retryService,
                JsonMapper.builder().build(), clock);
    }

    private OnboardingSession sessionWithId() {
        OnboardingSession session = new OnboardingSession(userId, "Roza", "Hakobyan");
        session.setId(UUID.randomUUID());
        return session;
    }

    @Test
    void createSessionFromRegistration_createsWhenAbsentAndPublishes() {
        when(sessionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.empty());
        when(sessionRepository.save(any(OnboardingSession.class))).thenAnswer(i -> {
            OnboardingSession s = i.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        OnboardingSession session = service.createSessionFromRegistration(userId, "Roza", "Hakobyan");

        assertThat(session.getFirstName()).isEqualTo("Roza");
        verify(events).publish(eq(KafkaTopics.ONBOARDING_STARTED), eq(userId), any());
    }

    @Test
    void createSessionFromRegistration_isIdempotent() {
        when(sessionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(sessionWithId()));

        service.createSessionFromRegistration(userId, "Roza", "Hakobyan");

        verify(sessionRepository, never()).save(any());
        verify(events, never()).publish(any(), any(), any());
    }

    @Test
    void submitGoal_generatesAssessmentAndPublishesEvent() {
        OnboardingSession session = sessionWithId();
        when(sessionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(OnboardingSession.class))).thenAnswer(i -> i.getArgument(0));
        when(assessmentRepository.findFirstBySessionIdOrderByCreatedAtDesc(session.getId()))
                .thenReturn(Optional.empty());
        when(generation.generateAssessment(userId, "I want to learn Java"))
                .thenReturn(new AssessmentQuestions(List.of("What is a variable?", "Have you coded?")));

        AssessmentResponse response = service.submitGoal(userId, new SubmitGoalRequest("I want to learn Java"));

        assertThat(response.questions()).hasSize(2);
        verify(events).publish(eq(KafkaTopics.GOAL_DISCOVERED), eq(userId), any());
    }

    @Test
    void submitGoal_whenAiUnavailable_schedulesRetryAndReturnsFallback() {
        OnboardingSession session = sessionWithId();
        when(sessionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(OnboardingSession.class))).thenAnswer(i -> i.getArgument(0));
        when(generation.generateAssessment(any(), any())).thenThrow(new AiException("LM down"));
        AiRequestRetry retry = new AiRequestRetry("ONBOARDING_ASSESSMENT", userId, session.getId().toString());
        retry.setId(UUID.randomUUID());
        when(retryService.record(any(), eq(userId), any())).thenReturn(retry);

        assertThatThrownBy(() -> service.submitGoal(userId, new SubmitGoalRequest("learn java")))
                .isInstanceOf(AiTemporarilyUnavailableException.class);

        verify(retryService).record(any(), eq(userId), any());
    }
}
