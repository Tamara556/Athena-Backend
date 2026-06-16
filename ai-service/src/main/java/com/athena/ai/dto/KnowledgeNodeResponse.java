package com.athena.ai.dto;

import java.time.Instant;

public record KnowledgeNodeResponse(
        String skillName,
        String domain,
        int masteryPercentage,
        double confidenceScore,
        Instant updatedAt
) {
}
