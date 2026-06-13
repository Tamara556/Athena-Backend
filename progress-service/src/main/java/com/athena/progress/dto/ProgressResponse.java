package com.athena.progress.dto;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProgressResponse(
        UUID userId,
        int totalCompletedTasks,
        long totalMinutes,
        int currentStreak,
        int longestStreak,
        LocalDate lastActivityDate,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {
}
