package com.athena.learning.dto;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        UUID userId,
        UUID taskId,
        Instant startedAt,
        Instant completedAt,
        long durationMinutes
) {
}
