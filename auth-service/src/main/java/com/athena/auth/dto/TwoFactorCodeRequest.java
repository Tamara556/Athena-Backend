package com.athena.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TwoFactorCodeRequest(
        @NotBlank(message = "Verification code is required")
        String code
) {
}
