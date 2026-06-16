package com.athena.common.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DailyPlanGeneratedEvent(
        UUID userId,
        UUID dailyPlanId,
        LocalDate planDate,
        Instant occurredAt
) {
}
