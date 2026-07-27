package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

public record MemoryDocumentIndexedEvent(
        UUID userId,
        UUID documentId,
        String sourceType,
        int chunkCount,
        Instant occurredAt
) {
}
