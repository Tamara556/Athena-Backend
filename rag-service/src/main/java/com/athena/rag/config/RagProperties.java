package com.athena.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "athena.rag")
public record RagProperties(
        @DefaultValue("1024") int embeddingDimension,
        @DefaultValue("900") int chunkMaxTokens,
        @DefaultValue("150") int chunkOverlapTokens,
        @DefaultValue("6") int retrievalTopK,
        @DefaultValue("0.35") double minSimilarity,
        @DefaultValue("6000") int maxContextTokens,
        @DefaultValue("20") int maxSearchResults
) {
}
