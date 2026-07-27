package com.athena.ai.generation.model;

import java.util.List;

public record InterviewEvaluation(
        int score,
        boolean passed,
        List<String> weaknesses,
        List<String> recommendations
) {
}
