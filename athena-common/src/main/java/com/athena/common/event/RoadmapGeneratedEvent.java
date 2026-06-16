package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

public record RoadmapGeneratedEvent(
        UUID userId,
        UUID roadmapId,
        Instant occurredAt
) {
}
