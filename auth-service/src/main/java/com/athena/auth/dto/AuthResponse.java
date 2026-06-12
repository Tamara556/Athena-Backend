package com.athena.auth.dto;

import java.util.Set;
import java.util.UUID;

/**
 * Issued on successful register / login / refresh. {@code expiresIn} is the
 * access-token lifetime in seconds.
 */
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
