package com.athena.rag.memory.repository;

import java.util.UUID;

public record VectorMatch(
        UUID chunkId,
        UUID documentId,
        UUID userId,
        String sourceType,
        UUID entityId,
        String learningDomain,
        String category,
        String title,
        String content,
        double score
) {
}
