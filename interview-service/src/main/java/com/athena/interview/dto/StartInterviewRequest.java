package com.athena.interview.dto;

import com.athena.interview.constants.InterviewConstants;
import jakarta.validation.constraints.NotBlank;

public record StartInterviewRequest(

        @NotBlank(message = InterviewConstants.DOMAIN_REQUIRED)
        String domain,

        @NotBlank(message = InterviewConstants.LEVEL_REQUIRED)
        String level
) {
}
