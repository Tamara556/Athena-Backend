package com.athena.user.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateUserProfileRequest(

        @NotNull(message = "userId is required")
        UUID userId,

        @NotBlank(message = "name is required")
        @Size(max = 120, message = "name must be at most 120 characters")
        String name,

        @Min(value = 5, message = "age must be at least 5")
        @Max(value = 120, message = "age must be at most 120")
        int age,

        @NotBlank(message = "goal is required")
        @Size(max = 500, message = "goal must be at most 500 characters")
        String goal,

        @DecimalMin(value = "0.0", message = "dailyStudyHours cannot be negative")
        @DecimalMax(value = "24.0", message = "dailyStudyHours cannot exceed 24")
        double dailyStudyHours
) {
}
