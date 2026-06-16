package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

public record RecommendationGeneratedEvent(
        UUID userId,
        int count,
        Instant occurredAt
) {
}
