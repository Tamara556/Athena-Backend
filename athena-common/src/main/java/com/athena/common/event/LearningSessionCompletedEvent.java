package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

public record LearningSessionCompletedEvent(
        UUID userId,
        UUID sessionId,
        int nodeIndex,
        Instant occurredAt
) {
}
