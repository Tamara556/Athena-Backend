package com.athena.ai.knowledgegraph.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record SnapshotSummaryResponse(
        UUID id,
        Instant generatedAt,
        int averageMastery,
        String serializedGraph
) implements Serializable {
}
