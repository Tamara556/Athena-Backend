package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

public record LearningSessionStartedEvent(
        UUID userId,
        UUID sessionId,
        Instant occurredAt
) {
}
