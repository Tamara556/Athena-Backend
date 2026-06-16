package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeGraphSnapshotCreatedEvent(
        UUID userId,
        UUID snapshotId,
        int averageMastery,
        Instant occurredAt
) {
}
