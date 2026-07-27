package com.athena.auth.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record LoginActivityResponse(
        UUID id,
        String ipAddress,
        String userAgent,
        Instant createdAt
) implements Serializable {
}
