package com.athena.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record DeviceResponse(
        UUID id,
        String deviceLabel,
        String ipAddress,
        Instant createdAt,
        Instant lastSeenAt,
        boolean current) {
}
