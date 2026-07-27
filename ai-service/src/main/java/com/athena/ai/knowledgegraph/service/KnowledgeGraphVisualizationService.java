package com.athena.ai.knowledgegraph.service;

import com.athena.ai.knowledgegraph.dto.KnowledgeGraphVisualizationResponse;
import com.athena.ai.knowledgegraph.dto.SnapshotSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface KnowledgeGraphVisualizationService {

    KnowledgeGraphVisualizationResponse getVisualization(UUID userId);

    List<SnapshotSummaryResponse> getHistory(UUID userId);

    void onGraphChanged(UUID userId);
}
