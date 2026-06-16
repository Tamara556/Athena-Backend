package com.athena.ai.dto;

import com.athena.ai.constants.AiConstants;
import jakarta.validation.constraints.NotBlank;

public record AssessmentAnswer(

        @NotBlank(message = AiConstants.QUESTION_REQUIRED)
        String question,

        @NotBlank(message = AiConstants.ANSWER_REQUIRED)
        String answer
) {
}
