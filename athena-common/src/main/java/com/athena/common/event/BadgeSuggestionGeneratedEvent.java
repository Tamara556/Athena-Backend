package com.athena.common.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BadgeSuggestionGeneratedEvent(
        UUID userId,
        String domain,
        List<BadgeSuggestion> suggestions,
        Instant occurredAt
) {
}
