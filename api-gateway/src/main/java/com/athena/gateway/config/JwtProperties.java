package com.athena.gateway.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

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
