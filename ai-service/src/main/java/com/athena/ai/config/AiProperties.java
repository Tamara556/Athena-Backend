package com.athena.ai.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "athena.ai")
public record AiProperties(

        @NotBlank(message = "athena.ai.base-url must be configured")
        String baseUrl,

        @NotBlank(message = "athena.ai.model must be configured")
        String model,

        @DefaultValue("") String apiKey,
        @DefaultValue("5s") Duration connectTimeout,
        @DefaultValue("1200s") Duration readTimeout,
        @DefaultValue("2") int maxRetries,
        @DefaultValue("2s") Duration retryBackoff,
        @DefaultValue("0.4") double temperature,
        @DefaultValue("8192") int maxTokens
) {
}
