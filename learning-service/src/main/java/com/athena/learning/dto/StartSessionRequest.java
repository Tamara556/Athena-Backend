package com.athena.learning.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StartSessionRequest(

        @NotNull(message = "taskId is required")
        UUID taskId
) {
}
