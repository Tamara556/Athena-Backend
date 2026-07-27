package com.athena.rag.memory.service;

import com.athena.rag.memory.domain.DocumentStatus;
import com.athena.rag.memory.domain.SourceType;

import java.util.UUID;

public record IngestOutcome(
        UUID documentId,
        SourceType sourceType,
        int chunkCount,
        DocumentStatus status,
        boolean reused
) {
}
