package com.athena.learning.dto;

import com.athena.learning.domain.TaskStatus;
import com.athena.learning.domain.TaskType;

import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID planId,
        String title,
        String description,
        TaskType taskType,
        int estimatedMinutes,
        TaskStatus status,
        Instant completedAt,
        Instant createdAt
) {
}
