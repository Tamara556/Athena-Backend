package com.athena.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef"; // 32 bytes

    private final JwtService jwtService =
            new JwtService(SECRET, "athena-auth", Duration.ofMinutes(15), Duration.ofDays(30));

    @Test
    void accessToken_roundTripsSubjectAndRoles() {
        String token = jwtService.generateAccessToken("user-1", List.of("USER", "ADMIN"));

        Claims claims = jwtService.parseAndValidate(token, TokenType.ACCESS);

        assertEquals("user-1", jwtService.extractSubject(claims));
        assertEquals(List.of("USER", "ADMIN"), jwtService.extractRoles(claims));
    }

    @Test
    void accessToken_cannotBeUsedAsRefreshToken() {
        String access = jwtService.generateAccessToken("user-1", List.of("USER"));

        assertThrows(JwtException.class, () -> jwtService.parseAndValidate(access, TokenType.REFRESH));
    }

    @Test
    void refreshToken_validatesAsRefresh() {
        String refresh = jwtService.generateRefreshToken("user-1", List.of("USER"));

        Claims claims = jwtService.parseAndValidate(refresh, TokenType.REFRESH);

        assertEquals("user-1", jwtService.extractSubject(claims));
    }

    @Test
    void tokenSignedWithDifferentSecret_isRejected() {
        JwtService other = new JwtService("ffffffffffffffffffffffffffffffff", "athena-auth",
                Duration.ofMinutes(15), Duration.ofDays(30));
        String foreignToken = other.generateAccessToken("user-1", List.of("USER"));

        assertThrows(JwtException.class, () -> jwtService.parseAndValidate(foreignToken, TokenType.ACCESS));
    }

    @Test
    void shortSecret_isRejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class,
                () -> new JwtService("too-short", "athena-auth", Duration.ofMinutes(1), Duration.ofMinutes(1)));
    }

    @Test
    void accessTokenTtlSeconds_isExposed() {
        assertTrue(jwtService.getAccessTokenTtlSeconds() == 900L);
    }
}
