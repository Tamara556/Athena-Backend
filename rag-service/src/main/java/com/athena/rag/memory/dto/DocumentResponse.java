package com.athena.rag.memory.dto;

import com.athena.rag.memory.domain.DocumentStatus;
import com.athena.rag.memory.domain.SourceType;
import com.athena.rag.memory.service.IngestOutcome;

import java.util.UUID;

public record DocumentResponse(
        UUID documentId,
        SourceType sourceType,
        int chunkCount,
        DocumentStatus status,
        boolean reused
) {

    public static DocumentResponse from(IngestOutcome outcome) {
        return new DocumentResponse(outcome.documentId(), outcome.sourceType(),
                outcome.chunkCount(), outcome.status(), outcome.reused());
    }
}
