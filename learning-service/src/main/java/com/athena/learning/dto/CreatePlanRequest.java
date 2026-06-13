package com.athena.learning.dto;

import com.athena.learning.constants.LearningConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePlanRequest(

        @NotBlank(message = LearningConstants.TITLE_REQUIRED)
        @Size(max = LearningConstants.TITLE_MAX_LENGTH, message = LearningConstants.TITLE_MAX_LENGTH_MESSAGE)
        String title,

        @Size(max = LearningConstants.DESCRIPTION_MAX_LENGTH, message = LearningConstants.DESCRIPTION_MAX_LENGTH_MESSAGE)
        String description
) {
}
