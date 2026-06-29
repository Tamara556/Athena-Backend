package com.athena.ai.dto;

import java.io.Serializable;
import java.util.UUID;

public record ReadingMaterialResponse(
        UUID id,
        String title,
        String content,
        int estimatedMinutes,
        int orderIndex
) implements Serializable {
}
