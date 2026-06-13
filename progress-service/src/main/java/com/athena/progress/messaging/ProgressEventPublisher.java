package com.athena.progress.messaging;

import com.athena.common.event.KafkaTopics;
import com.athena.common.event.StreakUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProgressEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishStreakUpdated(StreakUpdatedEvent event) {
        String payload = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(KafkaTopics.STREAK_UPDATED, event.userId().toString(), payload);
        log.info("Published StreakUpdatedEvent userId={} streak={} tasks={}",
                event.userId(), event.currentStreak(), event.completedTasks());
    }
}
