package com.athena.ai.dto;

import java.util.UUID;

public record OnboardingStateResponse(
        UUID sessionId,
        String status,
        String greeting,
        String goalText,
        String domain,
        String level,
        Integer estimatedMonths,
        Double dailyHours
) {
}
