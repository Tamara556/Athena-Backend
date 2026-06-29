package com.athena.ai.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LearningSessionResponse(
        UUID id,
        UUID roadmapId,
        UUID roadmapNodeId,
        int nodeIndex,
        String title,
        String status,
        int estimatedMinutes,
        Instant generatedAt,
        Instant updatedAt,
        List<ReadingMaterialResponse> readings,
        List<WatchingMaterialResponse> watchings,
        List<PracticeActivityResponse> practices,
        List<QuizQuestionResponse> quizzes
) implements Serializable {
}
