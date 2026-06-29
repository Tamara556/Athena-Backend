package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

public record LearningSessionGeneratedEvent(
        UUID userId,
        UUID sessionId,
        UUID roadmapId,
        int nodeIndex,
        Instant occurredAt
) {
}
