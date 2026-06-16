package com.athena.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GenerateInterviewQuestionsRequest(

        @NotNull(message = "userId is required")
        UUID userId,

        @NotBlank(message = "domain is required")
        String domain,

        @NotBlank(message = "level is required")
        String level
) {
}
