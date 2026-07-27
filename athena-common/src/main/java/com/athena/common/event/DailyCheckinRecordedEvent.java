package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

public record DailyCheckinRecordedEvent(
        UUID userId,
        UUID missionId,
        String confidence,
        Instant occurredAt
) {
}
