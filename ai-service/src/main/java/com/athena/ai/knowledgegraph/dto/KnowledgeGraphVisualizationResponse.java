package com.athena.ai.knowledgegraph.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

public record KnowledgeGraphVisualizationResponse(
        String domain,
        Instant generatedAt,
        List<GraphNode> nodes,
        List<GraphEdge> edges,
        GraphSummary summary,
        List<String> insights
) implements Serializable {

    public record GraphNode(
            String id,
            String label,
            int mastery,
            int confidence,
            String status,
            String category
    ) implements Serializable {
    }

    public record GraphEdge(
            String source,
            String target,
            String relationship
    ) implements Serializable {
    }

    public record GraphSummary(
            List<String> strongestSkills,
            List<String> weakestSkills,
            int averageMastery,
            int totalSkills
    ) implements Serializable {
    }
}
