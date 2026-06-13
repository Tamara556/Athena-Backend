package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by progress-service after a task completion is processed. Carries the
 * recomputed streak <em>and</em> cumulative completed-task count so badge-service
 * can evaluate both streak-based and count-based achievements from one event.
 */
public record StreakUpdatedEvent(
        UUID userId,
        int currentStreak,
        int longestStreak,
        int completedTasks,
        Instant occurredAt
) {
}
