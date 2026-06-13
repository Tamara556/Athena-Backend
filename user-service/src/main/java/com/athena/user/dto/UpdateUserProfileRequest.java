package com.athena.user.dto;

import com.athena.user.constants.UserConstants;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(

        @NotBlank(message = UserConstants.NAME_REQUIRED)
        @Size(max = UserConstants.NAME_MAX_LENGTH, message = UserConstants.NAME_MAX_LENGTH_MESSAGE)
        String name,

        @Min(value = UserConstants.AGE_MIN, message = UserConstants.AGE_MIN_MESSAGE)
        @Max(value = UserConstants.AGE_MAX, message = UserConstants.AGE_MAX_MESSAGE)
        int age,

        @NotBlank(message = UserConstants.GOAL_REQUIRED)
        @Size(max = UserConstants.GOAL_MAX_LENGTH, message = UserConstants.GOAL_MAX_LENGTH_MESSAGE)
        String goal,

        @DecimalMin(value = UserConstants.DAILY_STUDY_HOURS_MIN, message = UserConstants.DAILY_STUDY_HOURS_NEGATIVE)
        @DecimalMax(value = UserConstants.DAILY_STUDY_HOURS_MAX, message = UserConstants.DAILY_STUDY_HOURS_MAX_MESSAGE)
        double dailyStudyHours
) {
}
