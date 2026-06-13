package com.athena.learning.dto;

import java.time.Instant;
import java.util.UUID;

public record PlanResponse(
        UUID id,
        UUID userId,
        String title,
        String description,
        Instant createdAt
) {
}
