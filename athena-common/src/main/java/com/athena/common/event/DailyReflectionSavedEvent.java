package com.athena.common.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DailyReflectionSavedEvent(
        UUID userId,
        UUID missionId,
        LocalDate reflectionDate,
        boolean skipped,
        Instant occurredAt
) {
}
