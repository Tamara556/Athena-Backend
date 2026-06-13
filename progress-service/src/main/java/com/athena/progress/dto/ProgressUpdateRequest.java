package com.athena.progress.dto;

import com.athena.progress.constants.ProgressConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Reports a learning session. Values are <em>increments</em> applied to the
 * user's running totals and to today's daily record.
 */
public record ProgressUpdateRequest(

        @NotNull(message = ProgressConstants.USER_ID_REQUIRED)
        UUID userId,

        @Min(value = ProgressConstants.TASKS_COMPLETED_MIN, message = ProgressConstants.TASKS_COMPLETED_NEGATIVE)
        @Max(value = ProgressConstants.TASKS_COMPLETED_MAX, message = ProgressConstants.TASKS_COMPLETED_MAX_MESSAGE)
        int tasksCompleted,

        @Min(value = ProgressConstants.MINUTES_SPENT_MIN, message = ProgressConstants.MINUTES_SPENT_NEGATIVE)
        @Max(value = ProgressConstants.MAX_MINUTES_PER_DAY, message = ProgressConstants.MINUTES_SPENT_MAX_MESSAGE)
        int minutesSpent
) {

    public boolean hasActivity() {
        return tasksCompleted > 0 || minutesSpent > 0;
    }
}
