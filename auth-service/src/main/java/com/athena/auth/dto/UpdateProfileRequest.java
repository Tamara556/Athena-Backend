package com.athena.auth.dto;

import com.athena.auth.constants.AuthConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = AuthConstants.FIRST_NAME_REQUIRED)
        @Size(max = AuthConstants.FIRST_NAME_MAX_LENGTH, message = AuthConstants.FIRST_NAME_MAX_LENGTH_MESSAGE)
        String firstName,

        @NotBlank(message = AuthConstants.LAST_NAME_REQUIRED)
        @Size(max = AuthConstants.LAST_NAME_MAX_LENGTH, message = AuthConstants.LAST_NAME_MAX_LENGTH_MESSAGE)
        String lastName
) {
}
