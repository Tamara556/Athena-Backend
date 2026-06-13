package com.athena.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "login is required (email or username)")
        String login,

        @NotBlank(message = "password is required")
        String password
) {
}
