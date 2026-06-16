package com.athena.ai.service;

import com.athena.ai.dto.KnowledgeGraphVisualizationResponse;
import com.athena.ai.dto.SnapshotSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface KnowledgeGraphVisualizationService {

    KnowledgeGraphVisualizationResponse getVisualization(UUID userId);

    List<SnapshotSummaryResponse> getHistory(UUID userId);

    void onGraphChanged(UUID userId);
}
