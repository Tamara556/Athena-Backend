package com.athena.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TwoFactorVerifyRequest(
        @NotBlank(message = "Challenge token is required")
        String challengeToken,

        @NotBlank(message = "Verification code is required")
        String code
) {
}
