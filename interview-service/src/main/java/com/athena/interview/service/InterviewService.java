package com.athena.interview.service;

import com.athena.common.event.InterviewCompletedEvent;
import com.athena.common.event.InterviewEvaluatedEvent;
import com.athena.common.event.InterviewStartedEvent;
import com.athena.common.event.KafkaTopics;
import com.athena.common.exception.ResourceNotFoundException;
import com.athena.interview.client.AiDtos;
import com.athena.interview.client.AiInterviewClient;
import com.athena.interview.constants.InterviewConstants;
import com.athena.interview.domain.InterviewStatus;
import com.athena.interview.dto.InterviewResponse;
import com.athena.interview.dto.InterviewResultResponse;
import com.athena.interview.dto.StartInterviewRequest;
import com.athena.interview.dto.SubmitInterviewRequest;
import com.athena.interview.entity.Interview;
import com.athena.interview.entity.InterviewQuestion;
import com.athena.interview.entity.InterviewResult;
import com.athena.interview.messaging.InterviewEventPublisher;
import com.athena.interview.repository.InterviewRepository;
import com.athena.interview.repository.InterviewResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final InterviewResultRepository resultRepository;
    private final AiInterviewClient aiClient;
    private final InterviewEventPublisher events;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional
    public InterviewResponse start(UUID userId, StartInterviewRequest request) {
        Interview interview = new Interview(userId, request.domain(), request.level());

        AiDtos.AiQuestions generated = aiClient.generateQuestions(
                new AiDtos.AiQuestionsRequest(userId, request.domain(), request.level()));
        generated.questions().forEach(q -> interview.addQuestion(new InterviewQuestion(q.type(), q.question())));

        Interview saved = interviewRepository.save(interview);
        events.publish(KafkaTopics.INTERVIEW_STARTED, userId,
                new InterviewStartedEvent(userId, saved.getId(), Instant.now(clock)));
        log.info("Interview started interviewId={} userId={} questions={}",
                saved.getId(), userId, saved.getQuestions().size());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> getForUser(UUID userId) {
        return interviewRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InterviewResponse getById(UUID id) {
        return toResponse(loadInterview(id));
    }

    @Transactional
    public InterviewResultResponse submit(UUID userId, UUID interviewId, SubmitInterviewRequest request) {
        Interview interview = loadInterview(interviewId);
        if (!interview.getUserId().equals(userId)) {
            throw ResourceNotFoundException.of(InterviewConstants.RESOURCE_INTERVIEW, interviewId);
        }

        Map<UUID, String> questionTextById = interview.getQuestions().stream()
                .collect(Collectors.toMap(InterviewQuestion::getId, InterviewQuestion::getQuestionText));

        List<AiDtos.AiQnA> qna = request.answers().stream()
                .filter(a -> questionTextById.containsKey(a.questionId()))
                .map(a -> new AiDtos.AiQnA(questionTextById.get(a.questionId()), a.answer()))
                .toList();

        AiDtos.AiEvaluation evaluation = aiClient.evaluate(
                new AiDtos.AiEvaluateRequest(userId, interview.getDomain(), qna));

        InterviewResult result = resultRepository.save(new InterviewResult(
                interviewId, userId, evaluation.score(), evaluation.passed(),
                writeJson(evaluation.weaknesses()), writeJson(evaluation.recommendations())));

        interview.setStatus(InterviewStatus.EVALUATED);
        interviewRepository.save(interview);

        events.publish(KafkaTopics.INTERVIEW_COMPLETED, userId,
                new InterviewCompletedEvent(userId, interviewId, Instant.now(clock)));
        events.publish(KafkaTopics.INTERVIEW_EVALUATED, userId,
                new InterviewEvaluatedEvent(userId, interviewId, interview.getDomain(), evaluation.score(),
                        evaluation.passed(), evaluation.weaknesses(), Instant.now(clock)));
        log.info("Interview evaluated interviewId={} userId={} score={} passed={}",
                interviewId, userId, evaluation.score(), evaluation.passed());

        return new InterviewResultResponse(result.getId(), interviewId, evaluation.score(), evaluation.passed(),
                evaluation.weaknesses(), evaluation.recommendations(), result.getCreatedAt());
    }

    private Interview loadInterview(UUID id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of(InterviewConstants.RESOURCE_INTERVIEW, id));
    }

    private InterviewResponse toResponse(Interview interview) {
        List<InterviewResponse.QuestionView> questions = interview.getQuestions().stream()
                .map(q -> new InterviewResponse.QuestionView(q.getId(), q.getType(), q.getQuestionText()))
                .toList();
        return new InterviewResponse(interview.getId(), interview.getDomain(), interview.getLevel(),
                interview.getStatus().name(), questions, interview.getCreatedAt());
    }

    private String writeJson(Object value) {
        return objectMapper.writeValueAsString(value);
    }
}
