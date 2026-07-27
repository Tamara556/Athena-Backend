package com.athena.auth.dto;

import com.athena.auth.constants.AuthConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = AuthConstants.CURRENT_PASSWORD_REQUIRED)
        String currentPassword,

        @NotBlank(message = AuthConstants.PASSWORD_REQUIRED)
        @Size(min = AuthConstants.PASSWORD_MIN_LENGTH, max = AuthConstants.PASSWORD_MAX_LENGTH,
                message = AuthConstants.PASSWORD_SIZE_MESSAGE)
        @Pattern(regexp = AuthConstants.PASSWORD_PATTERN, message = AuthConstants.PASSWORD_COMPLEXITY_MESSAGE)
        String newPassword
) {
}
