package com.athena.learning.dto;

import com.athena.learning.constants.LearningConstants;
import com.athena.learning.domain.TaskType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateTaskRequest(

        @NotNull(message = LearningConstants.PLAN_ID_REQUIRED)
        UUID planId,

        @NotBlank(message = LearningConstants.TITLE_REQUIRED)
        @Size(max = LearningConstants.TITLE_MAX_LENGTH, message = LearningConstants.TITLE_MAX_LENGTH_MESSAGE)
        String title,

        @Size(max = LearningConstants.DESCRIPTION_MAX_LENGTH, message = LearningConstants.DESCRIPTION_MAX_LENGTH_MESSAGE)
        String description,

        @NotNull(message = LearningConstants.TASK_TYPE_REQUIRED)
        TaskType taskType,

        @Min(value = LearningConstants.ESTIMATED_MINUTES_MIN, message = LearningConstants.ESTIMATED_MINUTES_MIN_MESSAGE)
        @Max(value = LearningConstants.MAX_MINUTES_PER_DAY, message = LearningConstants.ESTIMATED_MINUTES_MAX_MESSAGE)
        int estimatedMinutes
) {
}
