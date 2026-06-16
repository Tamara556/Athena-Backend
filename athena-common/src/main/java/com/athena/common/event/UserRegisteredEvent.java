package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        Instant occurredAt
) {
}
