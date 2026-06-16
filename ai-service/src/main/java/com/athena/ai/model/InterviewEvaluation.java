package com.athena.ai.model;

import java.util.List;

public record InterviewEvaluation(
        int score,
        boolean passed,
        List<String> weaknesses,
        List<String> recommendations
) {
}
