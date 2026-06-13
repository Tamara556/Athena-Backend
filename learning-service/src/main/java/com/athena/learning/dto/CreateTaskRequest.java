package com.athena.learning.dto;

import com.athena.learning.domain.TaskType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateTaskRequest(

        @NotNull(message = "planId is required")
        UUID planId,

        @NotBlank(message = "title is required")
        @Size(max = 150, message = "title must be at most 150 characters")
        String title,

        @Size(max = 2000, message = "description must be at most 2000 characters")
        String description,

        @NotNull(message = "taskType is required")
        TaskType taskType,

        @Min(value = 1, message = "estimatedMinutes must be at least 1")
        @Max(value = 1440, message = "estimatedMinutes cannot exceed one day (1440)")
        int estimatedMinutes
) {
}
