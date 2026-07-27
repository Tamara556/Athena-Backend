package com.athena.auth.dto;

import com.athena.auth.constants.AuthConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ChangeEmailRequest(
        @NotBlank(message = AuthConstants.EMAIL_REQUIRED)
        @Email(message = AuthConstants.EMAIL_INVALID)
        String newEmail,

        @NotBlank(message = AuthConstants.CURRENT_PASSWORD_REQUIRED)
        String currentPassword
) {
}
