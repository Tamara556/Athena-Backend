package com.athena.ai.model;

import java.util.List;

public record LearningSessionContent(
        List<Reading> readings,
        List<Watching> watchings,
        List<Practice> practices,
        List<Quiz> quizzes
) {

    public record Reading(String title, String content, int estimatedMinutes) {
    }

    public record Watching(String title, String description, String videoQuery, int estimatedMinutes) {
    }

    public record Practice(String title, String description, String practiceType,
                           String instructions, String starterContent, int estimatedMinutes) {
    }

    public record Quiz(String question, String type, List<String> options,
                       String correctAnswer, String explanation) {
    }
}
