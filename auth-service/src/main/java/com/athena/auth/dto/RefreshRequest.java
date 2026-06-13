package com.athena.auth.dto;

import com.athena.auth.constants.AuthConstants;
import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(

        @NotBlank(message = AuthConstants.REFRESH_TOKEN_REQUIRED)
        String refreshToken
) {
}
