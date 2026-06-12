package com.athena.progress.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Reports a learning session. Values are <em>increments</em> applied to the
 * user's running totals and to today's daily record.
 */
public record ProgressUpdateRequest(

        @NotNull(message = "userId is required")
        UUID userId,

        @Min(value = 0, message = "tasksCompleted cannot be negative")
        @Max(value = 1000, message = "tasksCompleted per update is unrealistically high")
        int tasksCompleted,

        @Min(value = 0, message = "minutesSpent cannot be negative")
        @Max(value = 1440, message = "minutesSpent cannot exceed one day (1440)")
        int minutesSpent
) {

    public boolean hasActivity() {
        return tasksCompleted > 0 || minutesSpent > 0;
    }
}
