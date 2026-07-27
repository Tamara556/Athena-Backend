package com.athena.ai.interview.dto;

import com.athena.ai.constants.AiConstants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record EvaluateInterviewRequest(

        @NotNull(message = AiConstants.USER_ID_REQUIRED)
        UUID userId,

        @NotBlank(message = AiConstants.DOMAIN_REQUIRED)
        String domain,

        @NotEmpty(message = AiConstants.ANSWERS_NOT_EMPTY)
        @Valid
        List<QnA> answers
) {

    public record QnA(
            @NotBlank(message = AiConstants.QUESTION_REQUIRED) String question,
            @NotBlank(message = AiConstants.ANSWER_REQUIRED) String answer
    ) {
    }
}
