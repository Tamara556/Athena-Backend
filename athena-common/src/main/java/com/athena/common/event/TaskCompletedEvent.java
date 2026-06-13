package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

public record TaskCompletedEvent(
        UUID userId,
        UUID taskId,
        UUID planId,
        String taskType,
        int durationMinutes,
        Instant completedAt
) {
}
