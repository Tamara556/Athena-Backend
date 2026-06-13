package com.athena.learning.dto;

import com.athena.learning.constants.LearningConstants;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StartSessionRequest(

        @NotNull(message = LearningConstants.TASK_ID_REQUIRED)
        UUID taskId
) {
}
