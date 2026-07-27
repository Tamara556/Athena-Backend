package com.athena.progress.messaging;

import com.athena.common.event.InterviewEvaluatedEvent;
import com.athena.common.event.KafkaTopics;
import com.athena.common.event.StreakUpdatedEvent;
import com.athena.common.event.TaskCompletedEvent;
import com.athena.progress.dto.ProgressResponse;
import com.athena.progress.dto.ProgressUpdateRequest;
import com.athena.progress.service.ProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProgressEventConsumer {

    private final ProgressService progressService;
    private final ProgressEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.TASK_COMPLETED, groupId = "${spring.kafka.consumer.group-id}")
    public void onTaskCompleted(String payload) {
        TaskCompletedEvent event = objectMapper.readValue(payload, TaskCompletedEvent.class);
        log.info("Received TaskCompletedEvent userId={} taskId={} minutes={}",
                event.userId(), event.taskId(), event.durationMinutes());

        ProgressResponse progress = progressService.update(
                new ProgressUpdateRequest(event.userId(), 1, event.durationMinutes()));

        eventPublisher.publishStreakUpdated(new StreakUpdatedEvent(
                progress.userId(),
                progress.currentStreak(),
                progress.longestStreak(),
                progress.totalCompletedTasks(),
                Instant.now()));
    }

    @KafkaListener(topics = KafkaTopics.INTERVIEW_EVALUATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onInterviewEvaluated(String payload) {
        InterviewEvaluatedEvent event = objectMapper.readValue(payload, InterviewEvaluatedEvent.class);
        log.info("Received InterviewEvaluatedEvent userId={} interviewId={}", event.userId(), event.interviewId());
        progressService.recordInterview(event.userId());
    }
}
