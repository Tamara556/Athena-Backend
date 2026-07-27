package com.athena.ai.generation.dto;

import java.util.UUID;

public record RetryOutcomeResponse(UUID retryId, String status, String message) {
}
