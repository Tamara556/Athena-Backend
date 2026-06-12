package com.athena.auth.dto;

import java.util.Set;
import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String email,
        Set<String> roles,
        String tokenType,
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
