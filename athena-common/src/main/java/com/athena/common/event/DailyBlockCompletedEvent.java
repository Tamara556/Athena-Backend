package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

public record DailyBlockCompletedEvent(
        UUID userId,
        UUID missionId,
        UUID blockId,
        String blockType,
        int durationMinutes,
        UUID knowledgeNodeId,
        Instant occurredAt
) {
}
