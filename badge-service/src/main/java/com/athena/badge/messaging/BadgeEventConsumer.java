package com.athena.badge.messaging;

import com.athena.badge.dto.BadgeResponse;
import com.athena.badge.service.BadgeService;
import com.athena.common.event.BadgeAwardedEvent;
import com.athena.common.event.BadgeSuggestionGeneratedEvent;
import com.athena.common.event.KafkaTopics;
import com.athena.common.event.StreakUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BadgeEventConsumer {

    private final BadgeService badgeService;
    private final BadgeEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.STREAK_UPDATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onStreakUpdated(String payload) {
        StreakUpdatedEvent event = objectMapper.readValue(payload, StreakUpdatedEvent.class);
        log.info("Received StreakUpdatedEvent userId={} streak={} tasks={}",
                event.userId(), event.currentStreak(), event.completedTasks());

        List<BadgeResponse> awarded =
                badgeService.award(event.userId(), event.currentStreak(), event.completedTasks());

        for (BadgeResponse badge : awarded) {
            eventPublisher.publishBadgeAwarded(new BadgeAwardedEvent(event.userId(), badge.code(), Instant.now()));
        }
    }

    @KafkaListener(topics = KafkaTopics.BADGE_SUGGESTION_GENERATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onBadgeSuggestion(String payload) {
        BadgeSuggestionGeneratedEvent event = objectMapper.readValue(payload, BadgeSuggestionGeneratedEvent.class);
        log.info("Received BadgeSuggestionGeneratedEvent userId={} count={}",
                event.userId(), event.suggestions() == null ? 0 : event.suggestions().size());

        List<BadgeResponse> awarded = badgeService.awardSuggested(event.userId(), event.suggestions());

        for (BadgeResponse badge : awarded) {
            eventPublisher.publishBadgeAwarded(new BadgeAwardedEvent(event.userId(), badge.code(), Instant.now()));
        }
    }
}
