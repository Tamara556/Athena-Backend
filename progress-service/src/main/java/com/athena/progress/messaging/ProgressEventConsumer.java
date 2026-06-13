package com.athena.progress.messaging;

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

/**
 * Consumes {@link TaskCompletedEvent}, updates the user's metrics/streak, then
 * publishes a {@link StreakUpdatedEvent} for badge-service to react to.
 */
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

        // One completed task contributes 1 to the task count and its minutes to learning time.
        ProgressResponse progress = progressService.update(
                new ProgressUpdateRequest(event.userId(), 1, event.durationMinutes()));

        eventPublisher.publishStreakUpdated(new StreakUpdatedEvent(
                progress.userId(),
                progress.currentStreak(),
                progress.longestStreak(),
                progress.totalCompletedTasks(),
                Instant.now()));
    }
}
