package com.athena.interview.service;

import com.athena.common.event.KafkaTopics;
import com.athena.interview.client.AiDtos;
import com.athena.interview.client.AiInterviewClient;
import com.athena.interview.dto.InterviewResponse;
import com.athena.interview.dto.InterviewResultResponse;
import com.athena.interview.dto.StartInterviewRequest;
import com.athena.interview.dto.SubmitInterviewRequest;
import com.athena.interview.entity.Interview;
import com.athena.interview.entity.InterviewQuestion;
import com.athena.interview.messaging.InterviewEventPublisher;
import com.athena.interview.repository.InterviewRepository;
import com.athena.interview.repository.InterviewResultRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock private InterviewRepository interviewRepository;
    @Mock private InterviewResultRepository resultRepository;
    @Mock private AiInterviewClient aiClient;
    @Mock private InterviewEventPublisher events;

    private InterviewService service;

    private final UUID userId = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-13T10:00:00Z"), ZoneOffset.UTC);
        service = new InterviewService(interviewRepository, resultRepository, aiClient, events,
                JsonMapper.builder().build(), clock);
    }

    @Test
    void start_generatesQuestionsAndPublishesEvent() {
        when(aiClient.generateQuestions(any())).thenReturn(new AiDtos.AiQuestions(List.of(
                new AiDtos.AiQuestion("THEORY", "What is dependency injection?"),
                new AiDtos.AiQuestion("PRACTICAL", "Write a REST controller."))));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(i -> {
            Interview interview = i.getArgument(0);
            interview.setId(UUID.randomUUID());
            return interview;
        });

        InterviewResponse response = service.start(userId, new StartInterviewRequest("Java", "Intermediate"));

        assertThat(response.questions()).hasSize(2);
        assertThat(response.questions().getFirst().type()).isEqualTo("THEORY");
        verify(events).publish(eq(KafkaTopics.INTERVIEW_STARTED), eq(userId), any());
    }

    @Test
    void submit_evaluatesAndPersistsResult() {
        UUID interviewId = UUID.randomUUID();
        Interview interview = new Interview(userId, "Java", "Intermediate");
        interview.setId(interviewId);
        InterviewQuestion question = new InterviewQuestion("THEORY", "What is DI?");
        question.setId(UUID.randomUUID());
        interview.addQuestion(question);

        when(interviewRepository.findById(interviewId)).thenReturn(Optional.of(interview));
        when(aiClient.evaluate(any())).thenReturn(
                new AiDtos.AiEvaluation(82, true, List.of("Concurrency"), List.of("Review multithreading")));
        when(resultRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SubmitInterviewRequest request = new SubmitInterviewRequest(
                List.of(new SubmitInterviewRequest.AnswerInput(question.getId(), "Inversion of control")));

        InterviewResultResponse result = service.submit(userId, interviewId, request);

        assertThat(result.score()).isEqualTo(82);
        assertThat(result.passed()).isTrue();
        assertThat(result.weaknesses()).containsExactly("Concurrency");
        verify(events).publish(eq(KafkaTopics.INTERVIEW_COMPLETED), eq(userId), any());
        verify(events).publish(eq(KafkaTopics.INTERVIEW_EVALUATED), eq(userId), any());
    }
}
