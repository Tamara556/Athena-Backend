package com.athena.rag.memory.repository;

import java.time.Instant;
import java.util.UUID;

public record ChunkInsert(
        UUID id,
        UUID documentId,
        UUID userId,
        int chunkIndex,
        String content,
        int tokenCount,
        float[] embedding,
        String metadataJson,
        Instant createdAt
) {
}
