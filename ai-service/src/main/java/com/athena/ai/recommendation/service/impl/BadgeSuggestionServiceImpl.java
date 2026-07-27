package com.athena.ai.recommendation.service.impl;

import com.athena.ai.generation.messaging.AiEventPublisher;
import com.athena.ai.generation.service.AiGenerationService;
import com.athena.ai.recommendation.service.BadgeSuggestionService;
import com.athena.common.event.BadgeSuggestion;
import com.athena.common.event.BadgeSuggestionGeneratedEvent;
import com.athena.common.event.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeSuggestionServiceImpl implements BadgeSuggestionService {

    private final AiGenerationService generation;
    private final AiEventPublisher events;
    private final Clock clock;

    @Override
    public List<BadgeSuggestion> suggest(UUID userId, String domain) {
        List<BadgeSuggestion> suggestions = generation.generateBadgeSuggestions(userId, domain);
        events.publish(KafkaTopics.BADGE_SUGGESTION_GENERATED, userId,
                new BadgeSuggestionGeneratedEvent(userId, domain, suggestions, Instant.now(clock)));
        log.info("Generated {} badge suggestions userId={} domain={}", suggestions.size(), userId, domain);
        return suggestions;
    }
}
