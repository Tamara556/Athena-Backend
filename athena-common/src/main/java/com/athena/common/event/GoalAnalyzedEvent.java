package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

public record GoalAnalyzedEvent(
        UUID userId,
        UUID sessionId,
        String domain,
        String level,
        int estimatedMonths,
        Instant occurredAt
) {
}
