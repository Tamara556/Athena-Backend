package com.athena.rag.recommendation.dto;

import com.athena.rag.rag.dto.Citation;

import java.util.List;

public record NextRecommendationResponse(
        String recommendation,
        String rationale,
        List<String> focusAreas,
        boolean grounded,
        List<Citation> citations
) {
}
