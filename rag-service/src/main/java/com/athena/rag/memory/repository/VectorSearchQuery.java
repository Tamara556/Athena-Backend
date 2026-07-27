package com.athena.rag.memory.repository;

import java.util.List;
import java.util.UUID;

public record VectorSearchQuery(
        UUID userId,
        float[] queryVector,
        List<String> sourceTypes,
        String learningDomain,
        boolean includeGlobal,
        int limit,
        int offset
) {
}
