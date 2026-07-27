package com.athena.rag.retrieval.domain;

import java.util.UUID;

public record RetrievedChunk(
        UUID chunkId,
        UUID documentId,
        String sourceType,
        UUID entityId,
        String learningDomain,
        String category,
        String title,
        String content,
        double score
) {
}
