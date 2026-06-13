package com.athena.auth.dto;

import com.athena.auth.constants.AuthConstants;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = AuthConstants.LOGIN_REQUIRED)
        String login,

        @NotBlank(message = AuthConstants.PASSWORD_REQUIRED)
        String password
) {
}
