package com.athena.progress.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Rolling 7-day summary (inclusive of today).
 */
public record WeeklySummaryResponse(
        UUID userId,
        LocalDate weekStart,
        LocalDate weekEnd,
        int totalTasksCompleted,
        long totalMinutes,
        int activeDays,
        int currentStreak,
        List<DailyBreakdown> days
) {

    public record DailyBreakdown(LocalDate date, int tasksCompleted, int minutesSpent) {
    }
}
