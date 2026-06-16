package com.athena.ai.dto;

import com.athena.ai.constants.AiConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitGoalRequest(

        @NotBlank(message = AiConstants.GOAL_REQUIRED)
        @Size(max = AiConstants.GOAL_MAX_LENGTH, message = AiConstants.GOAL_MAX_LENGTH_MESSAGE)
        String goal
) {
}
