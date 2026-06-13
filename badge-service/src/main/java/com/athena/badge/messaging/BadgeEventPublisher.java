package com.athena.badge.messaging;

import com.athena.common.event.BadgeAwardedEvent;
import com.athena.common.event.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class BadgeEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishBadgeAwarded(BadgeAwardedEvent event) {
        String payload = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(KafkaTopics.BADGE_AWARDED, event.userId().toString(), payload);
        log.info("Published BadgeAwardedEvent userId={} badge={}", event.userId(), event.badgeCode());
    }
}
