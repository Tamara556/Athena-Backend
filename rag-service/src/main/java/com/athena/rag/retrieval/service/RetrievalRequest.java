package com.athena.rag.retrieval.service;

import java.util.List;
import java.util.UUID;

public record RetrievalRequest(
        UUID userId,
        String query,
        List<String> sourceTypes,
        String learningDomain,
        int topK,
        double minSimilarity,
        boolean includeGlobal,
        int offset
) {
}
