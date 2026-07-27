package com.athena.rag.memory.service;

import com.athena.rag.memory.domain.SourceType;
import com.athena.rag.memory.domain.Visibility;

import java.util.UUID;

public record MemoryIngestCommand(
        UUID userId,
        SourceType sourceType,
        UUID entityId,
        String learningDomain,
        String category,
        Visibility visibility,
        String title,
        String content
) {
}
