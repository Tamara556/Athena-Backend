package com.athena.ai.roadmap.controller;

import com.athena.ai.roadmap.dto.RoadmapResponse;
import com.athena.ai.roadmap.service.RoadmapService;
import com.athena.common.security.AuthHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/{id}")
    public ResponseEntity<RoadmapResponse> byId(@PathVariable UUID id) {
        return ResponseEntity.ok(roadmapService.getById(id));
    }
}
