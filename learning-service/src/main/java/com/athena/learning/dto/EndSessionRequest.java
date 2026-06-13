package com.athena.learning.dto;

import com.athena.learning.constants.LearningConstants;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EndSessionRequest(

        @NotNull(message = LearningConstants.SESSION_ID_REQUIRED)
        UUID sessionId
) {
}
