package com.athena.ai.knowledgegraph.controller;

import com.athena.ai.constants.AiConstants;
import com.athena.ai.knowledgegraph.dto.KnowledgeGraphVisualizationResponse;
import com.athena.ai.knowledgegraph.dto.KnowledgeNodeResponse;
import com.athena.ai.knowledgegraph.dto.SnapshotSummaryResponse;
import com.athena.ai.knowledgegraph.dto.UpdateKnowledgeRequest;
import com.athena.ai.knowledgegraph.service.KnowledgeGraphService;
import com.athena.ai.knowledgegraph.service.KnowledgeGraphVisualizationService;
import com.athena.ai.web.AccessForbiddenException;
import com.athena.common.security.AuthHeaders;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ai/knowledge-graph")
@RequiredArgsConstructor
public class KnowledgeGraphController {

    private final KnowledgeGraphService knowledgeGraphService;
    private final KnowledgeGraphVisualizationService visualizationService;
    private final MeterRegistry meterRegistry;

    @GetMapping("/me")
    public ResponseEntity<List<KnowledgeNodeResponse>> mine(@RequestHeader(AuthHeaders.USER_ID) UUID userId) {
        return ResponseEntity.ok(knowledgeGraphService.getForUser(userId));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<KnowledgeNodeResponse>> forUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(knowledgeGraphService.getForUser(userId));
    }

    @PostMapping("/update")
    public ResponseEntity<List<KnowledgeNodeResponse>> update(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                              @Valid @RequestBody UpdateKnowledgeRequest request) {
        return ResponseEntity.ok(knowledgeGraphService.applyUpdates(userId, request));
    }

    @GetMapping("/me/visualization")
    public ResponseEntity<KnowledgeGraphVisualizationResponse> myVisualization(
            @RequestHeader(AuthHeaders.USER_ID) UUID userId) {
        meterRegistry.counter(AiConstants.METRIC_VIZ_REQUESTS).increment();
        return ResponseEntity.ok(visualizationService.getVisualization(userId));
    }

    @GetMapping("/{userId}/visualization")
    public ResponseEntity<KnowledgeGraphVisualizationResponse> visualization(
            @PathVariable UUID userId,
            @RequestHeader(AuthHeaders.USER_ID) UUID callerId,
            @RequestHeader(value = AuthHeaders.USER_ROLES, required = false) String roles) {
        assertCanAccess(userId, callerId, roles);
        meterRegistry.counter(AiConstants.METRIC_VIZ_REQUESTS).increment();
        return ResponseEntity.ok(visualizationService.getVisualization(userId));
    }

    @GetMapping("/me/history")
    public ResponseEntity<List<SnapshotSummaryResponse>> myHistory(
            @RequestHeader(AuthHeaders.USER_ID) UUID userId) {
        return ResponseEntity.ok(visualizationService.getHistory(userId));
    }

    @GetMapping("/{userId}/history")
    public ResponseEntity<List<SnapshotSummaryResponse>> history(
            @PathVariable UUID userId,
            @RequestHeader(AuthHeaders.USER_ID) UUID callerId,
            @RequestHeader(value = AuthHeaders.USER_ROLES, required = false) String roles) {
        assertCanAccess(userId, callerId, roles);
        return ResponseEntity.ok(visualizationService.getHistory(userId));
    }

    private void assertCanAccess(UUID targetUserId, UUID callerId, String roles) {
        boolean isAdmin = roles != null && roles.contains("ADMIN");
        if (!targetUserId.equals(callerId) && !isAdmin) {
            throw new AccessForbiddenException("You may only access your own knowledge graph");
        }
    }
}
