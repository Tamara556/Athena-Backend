package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

public record LearningPlanCreatedEvent(
        UUID planId,
        UUID userId,
        String title,
        Instant createdAt
) {
}
