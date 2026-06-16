package com.athena.ai.dto;

import java.util.UUID;

public record RetryOutcomeResponse(UUID retryId, String status, String message) {
}
