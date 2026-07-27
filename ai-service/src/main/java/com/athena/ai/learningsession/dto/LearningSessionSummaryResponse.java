package com.athena.ai.learningsession.dto;

import java.util.UUID;

public record LearningSessionSummaryResponse(
        UUID id,
        UUID roadmapNodeId,
        int nodeIndex,
        String title,
        String status,
        int estimatedMinutes
) {
}
