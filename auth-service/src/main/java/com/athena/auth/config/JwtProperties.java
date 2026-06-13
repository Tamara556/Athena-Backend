package com.athena.auth.config;

import com.athena.auth.constants.AuthConstants;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "athena.security.jwt")
public record JwtProperties(

        @NotBlank(message = AuthConstants.JWT_SECRET_REQUIRED)
        String secret,

        @DefaultValue("athena-auth") String issuer,

        @DefaultValue("15m") Duration accessTokenTtl,

        @DefaultValue("30d") Duration refreshTokenTtl
) {
}
