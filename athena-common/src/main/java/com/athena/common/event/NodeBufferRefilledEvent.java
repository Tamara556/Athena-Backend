package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

public record NodeBufferRefilledEvent(
        UUID userId,
        UUID roadmapId,
        UUID sessionId,
        int nodeIndex,
        Instant occurredAt
) {
}
