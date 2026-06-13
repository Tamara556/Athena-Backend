package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

public record BadgeAwardedEvent(
        UUID userId,
        String badgeCode,
        Instant awardedAt
) {
}
