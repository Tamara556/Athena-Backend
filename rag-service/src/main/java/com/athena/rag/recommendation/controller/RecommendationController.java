package com.athena.rag.recommendation.controller;

import com.athena.common.security.AuthHeaders;
import com.athena.rag.recommendation.dto.NextRecommendationRequest;
import com.athena.rag.recommendation.dto.NextRecommendationResponse;
import com.athena.rag.recommendation.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @PostMapping("/rag/recommendations/next")
    public ResponseEntity<NextRecommendationResponse> next(
            @RequestHeader(AuthHeaders.USER_ID) UUID userId,
            @Valid @RequestBody(required = false) NextRecommendationRequest request) {
        return ResponseEntity.ok(recommendationService.recommendNext(userId, request));
    }
}
