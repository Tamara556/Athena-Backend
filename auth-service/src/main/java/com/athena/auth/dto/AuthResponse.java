package com.athena.auth.dto;

import java.util.Set;
import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String username,
        String firstName,
        String lastName,
        String email,
        Set<String> roles,
        String tokenType,
        String accessToken,
        String refreshToken,
        long expiresIn,
        String imageName,
        boolean twoFactorRequired,
        String challengeToken,
        String sessionId
) {

    public static AuthResponse twoFactorChallenge(String challengeToken) {
        return new AuthResponse(null, null, null, null, null, Set.of(), null, null, null, 0, null, true, challengeToken, null);
    }
}
