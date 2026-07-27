package com.athena.ai.learningsession.controller;

import com.athena.ai.learningsession.dto.LearningSessionResponse;
import com.athena.ai.learningsession.dto.LearningSessionSummaryResponse;
import com.athena.ai.learningsession.service.LearningSessionService;
import com.athena.common.security.AuthHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/learning-sessions")
@RequiredArgsConstructor
public class LearningSessionController {

    private final LearningSessionService learningSessionService;

    @GetMapping("/current")
    public ResponseEntity<LearningSessionResponse> current(@RequestHeader(AuthHeaders.USER_ID) UUID userId) {
        return ResponseEntity.ok(learningSessionService.getCurrent(userId));
    }

    @PostMapping("/generate")
    public ResponseEntity<LearningSessionResponse> generate(@RequestHeader(AuthHeaders.USER_ID) UUID userId) {
        return ResponseEntity.ok(learningSessionService.prepareCurrent(userId));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<LearningSessionSummaryResponse>> upcoming(
            @RequestHeader(AuthHeaders.USER_ID) UUID userId) {
        return ResponseEntity.ok(learningSessionService.getUpcoming(userId));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<LearningSessionResponse> byId(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                        @PathVariable UUID sessionId) {
        return ResponseEntity.ok(learningSessionService.getById(userId, sessionId));
    }

    @PostMapping("/{sessionId}/start")
    public ResponseEntity<LearningSessionResponse> start(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                         @PathVariable UUID sessionId) {
        return ResponseEntity.ok(learningSessionService.start(userId, sessionId));
    }

    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<LearningSessionResponse> complete(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(learningSessionService.complete(userId, sessionId));
    }
}
