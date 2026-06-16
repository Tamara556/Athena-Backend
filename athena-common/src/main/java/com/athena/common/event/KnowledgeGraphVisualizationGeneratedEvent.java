package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeGraphVisualizationGeneratedEvent(
        UUID userId,
        String domain,
        int totalSkills,
        int averageMastery,
        Instant occurredAt
) {
}
