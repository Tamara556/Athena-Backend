package com.athena.learning.messaging;

import com.athena.common.event.KafkaTopics;
import com.athena.common.event.LearningPlanCreatedEvent;
import com.athena.common.event.TaskCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class LearningEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishPlanCreated(LearningPlanCreatedEvent event) {
        publish(KafkaTopics.PLAN_CREATED, event.userId().toString(), event);
    }

    public void publishTaskCompleted(TaskCompletedEvent event) {
        publish(KafkaTopics.TASK_COMPLETED, event.userId().toString(), event);
    }

    private void publish(String topic, String key, Object event) {
        String payload = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(topic, key, payload);
        log.info("Published event to topic={} key={} type={}", topic, key, event.getClass().getSimpleName());
    }
}
