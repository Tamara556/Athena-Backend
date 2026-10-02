package com.athena.ai.roadmap.controller;

import com.athena.ai.roadmap.dto.RoadmapResponse;
import com.athena.ai.roadmap.service.RoadmapService;
import com.athena.common.security.AuthHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/ai/roadmaps")
@RequiredArgsConstructor
public class RoadmapController {

    private final RoadmapService roadmapService;

    @GetMapping("/me")
    public ResponseEntity<RoadmapResponse> myRoadmap(@RequestHeader(AuthHeaders.USER_ID) UUID userId) {
        return ResponseEntity.ok(roadmapService.getLatestForUser(userId));
    }

    /**
     * Internal callers (e.g. rag-service Feign) omit {@code X-User-Id} and may
     * read any roadmap by id. Gateway-authenticated requests always carry the
     * header, so ownership is enforced for browser/API clients.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RoadmapResponse> byId(@PathVariable UUID id,
                                                @RequestHeader(value = AuthHeaders.USER_ID, required = false) UUID userId) {
        return ResponseEntity.ok(userId == null
                ? roadmapService.getById(id)
                : roadmapService.getByIdForUser(id, userId));
    }

    @PostMapping("/me/phases/{index}/complete")
    public ResponseEntity<RoadmapResponse> completePhase(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                         @PathVariable int index) {
        return ResponseEntity.ok(roadmapService.completePhase(userId, index));
    }
}
