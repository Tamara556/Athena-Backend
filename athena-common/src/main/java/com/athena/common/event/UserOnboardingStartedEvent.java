package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

public record UserOnboardingStartedEvent(
        UUID userId,
        UUID sessionId,
        Instant occurredAt
) {
}
