package com.athena.ai.dto;

import java.util.UUID;

public record FallbackResponse(
        String status,
        String message,
        boolean retryAvailable,
        UUID retryId
) {

    public static FallbackResponse temporarilyUnavailable(UUID retryId) {
        return new FallbackResponse(
                "TEMPORARILY_UNAVAILABLE",
                "Athena is temporarily unavailable. Please try again shortly.",
                true,
                retryId);
    }
}
