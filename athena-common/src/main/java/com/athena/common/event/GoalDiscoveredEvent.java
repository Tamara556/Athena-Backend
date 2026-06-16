package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

public record GoalDiscoveredEvent(
        UUID userId,
        UUID sessionId,
        String goalText,
        Instant occurredAt
) {
}
