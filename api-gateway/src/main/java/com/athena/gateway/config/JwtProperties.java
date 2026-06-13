package com.athena.gateway.config;

import com.athena.gateway.constants.GatewayConstants;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "athena.security.jwt")
public record JwtProperties(

        @NotBlank(message = GatewayConstants.JWT_SECRET_REQUIRED)
        String secret,

        @DefaultValue("athena-auth") String issuer,

        @DefaultValue("15m") Duration accessTokenTtl,

        @DefaultValue("30d") Duration refreshTokenTtl
) {
}
