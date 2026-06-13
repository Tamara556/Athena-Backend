package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a learner completes a task. Consumed by progress-service to
 * update metrics and streaks.
 */
public record TaskCompletedEvent(
        UUID userId,
        UUID taskId,
        UUID planId,
        String taskType,
        int durationMinutes,
        Instant completedAt
) {
}
