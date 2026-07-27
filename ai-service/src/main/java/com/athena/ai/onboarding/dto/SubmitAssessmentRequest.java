package com.athena.ai.onboarding.dto;

import com.athena.ai.constants.AiConstants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitAssessmentRequest(

        @NotEmpty(message = AiConstants.ANSWERS_NOT_EMPTY)
        @Valid
        List<AssessmentAnswer> answers
) {
}
