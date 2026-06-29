package com.athena.ai.dto;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record QuizQuestionResponse(
        UUID id,
        String question,
        String type,
        List<String> options,
        String correctAnswer,
        String explanation,
        int orderIndex
) implements Serializable {
}
