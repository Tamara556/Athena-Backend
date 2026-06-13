package com.athena.auth.dto;

import com.athena.auth.constants.AuthConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = AuthConstants.FIRST_NAME_REQUIRED)
        @Size(max = AuthConstants.FIRST_NAME_MAX_LENGTH, message = AuthConstants.FIRST_NAME_MAX_LENGTH_MESSAGE)
        String firstName,

        @NotBlank(message = AuthConstants.LAST_NAME_REQUIRED)
        @Size(max = AuthConstants.LAST_NAME_MAX_LENGTH, message = AuthConstants.LAST_NAME_MAX_LENGTH_MESSAGE)
        String lastName,

        @NotBlank(message = AuthConstants.USERNAME_REQUIRED)
        @Pattern(regexp = AuthConstants.USERNAME_PATTERN, message = AuthConstants.USERNAME_PATTERN_MESSAGE)
        String username,

        @NotBlank(message = AuthConstants.EMAIL_REQUIRED)
        @Email(message = AuthConstants.EMAIL_INVALID)
        String email,

        @NotBlank(message = AuthConstants.PASSWORD_REQUIRED)
        @Size(
                min = AuthConstants.PASSWORD_MIN_LENGTH,
                max = AuthConstants.PASSWORD_MAX_LENGTH,
                message = AuthConstants.PASSWORD_SIZE_MESSAGE)
        @Pattern(regexp = AuthConstants.PASSWORD_PATTERN, message = AuthConstants.PASSWORD_COMPLEXITY_MESSAGE)
        String password
) {
}
