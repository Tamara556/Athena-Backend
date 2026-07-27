package com.athena.rag.rag.dto;

import java.util.UUID;

public record Citation(
        int index,
        String sourceType,
        UUID entityId,
        String title,
        double score
) {
}
