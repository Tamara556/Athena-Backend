package com.athena.ai.recommendation.controller;

import com.athena.ai.recommendation.dto.SuggestBadgesRequest;
import com.athena.ai.recommendation.service.BadgeSuggestionService;
import com.athena.common.event.BadgeSuggestion;
import com.athena.common.security.AuthHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ai/badges")
@RequiredArgsConstructor
public class AiBadgeController {

    private final BadgeSuggestionService badgeSuggestionService;

    @PostMapping("/suggest")
    public ResponseEntity<List<BadgeSuggestion>> suggest(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                         @Valid @RequestBody SuggestBadgesRequest request) {
        return ResponseEntity.ok(badgeSuggestionService.suggest(userId, request.domain()));
    }
}
