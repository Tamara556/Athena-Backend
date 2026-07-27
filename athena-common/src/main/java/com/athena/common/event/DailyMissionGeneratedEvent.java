package com.athena.common.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DailyMissionGeneratedEvent(
        UUID userId,
        UUID missionId,
        LocalDate missionDate,
        Instant occurredAt
) {
}
