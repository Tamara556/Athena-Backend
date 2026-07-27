package com.athena.ai.generation.model;

import java.util.List;

public record GoalAnalysis(
        String domain,
        String level,
        int estimatedMonths,
        double dailyHours,
        List<String> prerequisites
) {
}
