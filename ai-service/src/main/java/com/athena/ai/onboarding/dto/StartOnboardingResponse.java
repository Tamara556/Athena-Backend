package com.athena.ai.onboarding.dto;

import java.util.UUID;

public record StartOnboardingResponse(UUID sessionId, String greeting, String firstQuestion) {
}
