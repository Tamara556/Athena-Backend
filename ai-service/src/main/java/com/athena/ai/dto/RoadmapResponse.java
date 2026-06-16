package com.athena.ai.dto;

import com.athena.ai.model.RoadmapContent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoadmapResponse(
        UUID id,
        String goal,
        String level,
        List<RoadmapContent.Phase> phases,
        Instant createdAt
) {
}
