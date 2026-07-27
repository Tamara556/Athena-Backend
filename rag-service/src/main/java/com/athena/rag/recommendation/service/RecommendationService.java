package com.athena.rag.recommendation.service;

import com.athena.rag.recommendation.dto.NextRecommendationRequest;
import com.athena.rag.recommendation.dto.NextRecommendationResponse;

import java.util.UUID;

public interface RecommendationService {

    NextRecommendationResponse recommendNext(UUID userId, NextRecommendationRequest request);
}
