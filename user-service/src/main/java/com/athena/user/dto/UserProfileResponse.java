package com.athena.user.dto;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID userId,
        String name,
        int age,
        String goal,
        double dailyStudyHours,
        Instant createdAt,
        Instant updatedAt
) {
}
