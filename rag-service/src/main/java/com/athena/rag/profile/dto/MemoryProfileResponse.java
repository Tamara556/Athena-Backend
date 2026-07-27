package com.athena.rag.profile.dto;

import com.athena.rag.memory.domain.SourceType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MemoryProfileResponse(
        UUID userId,
        long totalDocuments,
        List<SourceCount> sources,
        List<MemoryItem> recentMemory,
        String knowledgeGraphSummary,
        String progressSummary
) {

    public record SourceCount(SourceType sourceType, long count) {
    }

    public record MemoryItem(
            SourceType sourceType,
            String title,
            UUID entityId,
            String learningDomain,
            Instant updatedAt
    ) {
    }
}
