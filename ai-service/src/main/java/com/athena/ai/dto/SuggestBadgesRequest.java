package com.athena.ai.dto;

import com.athena.ai.constants.AiConstants;
import jakarta.validation.constraints.NotBlank;

public record SuggestBadgesRequest(

        @NotBlank(message = AiConstants.DOMAIN_REQUIRED)
        String domain
) {
}
