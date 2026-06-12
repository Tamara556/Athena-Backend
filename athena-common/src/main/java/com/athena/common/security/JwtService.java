package com.athena.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public class JwtService {

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TYPE = "type";

    private final SecretKey signingKey;
    private final String issuer;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    public JwtService(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must be at least 256 bits (32 bytes) for HS256; got " + keyBytes.length + " bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = issuer;
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public String generateAccessToken(String subject, List<String> roles) {
        return generate(subject, roles, TokenType.ACCESS, accessTokenTtl);
    }

    public String generateRefreshToken(String subject, List<String> roles) {
        return generate(subject, roles, TokenType.REFRESH, refreshTokenTtl);
    }

    private String generate(String subject, List<String> roles, TokenType type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .issuer(issuer)
                .claim(CLAIM_TYPE, type.name())
                .claim(CLAIM_ROLES, roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseAndValidate(String token, TokenType expectedType) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token);

        Claims claims = jws.getPayload();
        String actualType = claims.get(CLAIM_TYPE, String.class);
        if (!expectedType.name().equals(actualType)) {
            throw new JwtException("Expected " + expectedType + " token but got " + actualType);
        }
        return claims;
    }

    public String extractSubject(Claims claims) {
        return claims.getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(Claims claims) {
        Object roles = claims.get(CLAIM_ROLES);
        return roles instanceof List<?> list ? (List<String>) list : List.of();
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }
}
