package com.athena.interview.client;

import java.util.List;
import java.util.UUID;

public final class AiDtos {

    public record AiQuestionsRequest(UUID userId, String domain, String level) {
    }

    public record AiQuestions(List<AiQuestion> questions) {
    }

    public record AiQuestion(String type, String question) {
    }

    public record AiEvaluateRequest(UUID userId, String domain, List<AiQnA> answers) {
    }

    public record AiQnA(String question, String answer) {
    }

    public record AiEvaluation(int score, boolean passed, List<String> weaknesses, List<String> recommendations) {
    }
}
