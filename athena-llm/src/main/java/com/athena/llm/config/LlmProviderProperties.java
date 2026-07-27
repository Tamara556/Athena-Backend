package com.athena.llm.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "athena.llm")
public record LlmProviderProperties(

        @DefaultValue Chat chat,

        @DefaultValue Embedding embedding
) {

    public record Chat(
            @NotBlank(message = "athena.llm.chat.base-url must be configured")
            @DefaultValue("http://localhost:1234/v1") String baseUrl,

            @NotBlank(message = "athena.llm.chat.model must be configured")
            @DefaultValue("qwen3-14b") String model,

            @DefaultValue("") String apiKey,
            @DefaultValue("5s") Duration connectTimeout,
            @DefaultValue("1200s") Duration readTimeout,
            @DefaultValue("2") int maxRetries,
            @DefaultValue("2s") Duration retryBackoff,
            @DefaultValue("0.4") double temperature,
            @DefaultValue("8192") int maxTokens
    ) {
    }

    public record Embedding(
            @NotBlank(message = "athena.llm.embedding.base-url must be configured")
            @DefaultValue("http://localhost:1234/v1") String baseUrl,

            @NotBlank(message = "athena.llm.embedding.model must be configured")
            @DefaultValue("text-embedding-bge-m3") String model,

            @DefaultValue("") String apiKey,
            @DefaultValue("1024") int dimension,
            @DefaultValue("5s") Duration connectTimeout,
            @DefaultValue("120s") Duration readTimeout,
            @DefaultValue("2") int maxRetries,
            @DefaultValue("2s") Duration retryBackoff,
            @DefaultValue("32") int batchSize
    ) {
    }
}
