package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

public record DailyBlockSkippedEvent(
        UUID userId,
        UUID missionId,
        UUID blockId,
        String blockType,
        String reason,
        Instant occurredAt
) {
}
