package com.athena.rag.recommendation.model;

import java.util.List;

public record NextRecommendationContent(
        String recommendation,
        String rationale,
        List<String> focusAreas
) {
}
