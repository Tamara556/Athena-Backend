package com.athena.auth.dto;

import com.athena.auth.constants.AuthConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TwoFactorSetupRequest(
        @NotBlank(message = AuthConstants.PHONE_REQUIRED)
        @Pattern(regexp = AuthConstants.PHONE_PATTERN, message = AuthConstants.PHONE_INVALID)
        String phoneNumber
) {
}
