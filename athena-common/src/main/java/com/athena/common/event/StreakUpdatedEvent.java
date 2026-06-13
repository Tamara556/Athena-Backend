package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

public record StreakUpdatedEvent(
        UUID userId,
        int currentStreak,
        int longestStreak,
        int completedTasks,
        Instant occurredAt
) {
}
