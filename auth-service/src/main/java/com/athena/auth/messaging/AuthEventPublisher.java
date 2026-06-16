package com.athena.auth.messaging;

import com.athena.common.event.KafkaTopics;
import com.athena.common.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishUserRegistered(UserRegisteredEvent event) {
        String payload = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(KafkaTopics.USER_REGISTERED, event.userId().toString(), payload);
        log.info("Published UserRegisteredEvent userId={}", event.userId());
    }
}
