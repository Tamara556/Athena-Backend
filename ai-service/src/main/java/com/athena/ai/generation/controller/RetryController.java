package com.athena.ai.generation.controller;

import com.athena.ai.generation.dto.RetryOutcomeResponse;
import com.athena.ai.generation.service.RetryDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/ai/retry")
@RequiredArgsConstructor
public class RetryController {

    private final RetryDispatcher retryDispatcher;

    @PostMapping("/{requestId}")
    public ResponseEntity<RetryOutcomeResponse> retry(@PathVariable UUID requestId) {
        return ResponseEntity.ok(retryDispatcher.retryNow(requestId));
    }
}
