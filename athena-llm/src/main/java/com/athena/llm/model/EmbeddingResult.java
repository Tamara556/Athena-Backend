package com.athena.llm.model;

import java.util.List;

public record EmbeddingResult(
        List<float[]> vectors,
        int dimension,
        int totalTokens,
        long latencyMs
) {
}
