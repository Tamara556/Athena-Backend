package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

public record DailyMissionAdjustedEvent(
        UUID userId,
        UUID missionId,
        String adjustmentType,
        String reason,
        UUID affectedBlockId,
        Instant occurredAt
) {
}
