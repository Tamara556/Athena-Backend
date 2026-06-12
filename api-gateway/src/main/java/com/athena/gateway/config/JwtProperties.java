package com.athena.gateway.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * The gateway only <em>verifies</em> tokens, so it shares the Auth Service's
 * {@code secret} and {@code issuer}. The TTLs are unused for verification but
 * kept for symmetry with the issuer's configuration.
 */
@Validated
@ConfigurationProperties(prefix = "athena.security.jwt")
public record JwtProperties(

        @NotBlank(message = "athena.security.jwt.secret must be configured")
        String secret,

        @DefaultValue("athena-auth") String issuer,

        @DefaultValue("15m") Duration accessTokenTtl,

        @DefaultValue("30d") Duration refreshTokenTtl
) {
}
