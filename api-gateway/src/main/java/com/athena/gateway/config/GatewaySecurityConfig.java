package com.athena.gateway.config;

import com.athena.common.security.JwtService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class GatewaySecurityConfig {

    @Bean
    public JwtService jwtService(JwtProperties properties) {
        return new JwtService(
                properties.secret(),
                properties.issuer(),
                properties.accessTokenTtl(),
                properties.refreshTokenTtl());
    }
}
