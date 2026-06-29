package com.athena.ai.dto;

import java.io.Serializable;
import java.util.UUID;

public record PracticeActivityResponse(
        UUID id,
        String title,
        String description,
        String practiceType,
        String instructions,
        String starterContent,
        int estimatedMinutes,
        int orderIndex
) implements Serializable {
}
