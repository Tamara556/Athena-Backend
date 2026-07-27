package com.athena.ai.generation.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publish(String topic, UUID userId, Object event) {
        String payload = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(topic, userId.toString(), payload);
        log.info("Published event topic={} userId={} type={}", topic, userId, event.getClass().getSimpleName());
    }
}
