package com.athena.learning.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EndSessionRequest(

        @NotNull(message = "sessionId is required")
        UUID sessionId
) {
}
