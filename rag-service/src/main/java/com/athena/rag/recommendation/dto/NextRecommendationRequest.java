package com.athena.rag.recommendation.dto;

import jakarta.validation.constraints.Size;

public record NextRecommendationRequest(
        @Size(max = 120, message = "domain must be at most 120 characters")
        String domain
) {
}
