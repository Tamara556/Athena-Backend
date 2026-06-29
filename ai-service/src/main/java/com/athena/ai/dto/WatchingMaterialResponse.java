package com.athena.ai.dto;

import java.io.Serializable;
import java.util.UUID;

public record WatchingMaterialResponse(
        UUID id,
        String title,
        String description,
        String videoQuery,
        int estimatedMinutes,
        int orderIndex
) implements Serializable {
}
