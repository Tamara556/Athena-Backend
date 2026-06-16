package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeGraphUpdatedEvent(
        UUID userId,
        Instant occurredAt
) {
}
