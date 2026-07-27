package com.athena.rag.client.dto;

import java.util.List;
import java.util.UUID;

public record RoadmapView(UUID id, String goal, String level, List<PhaseView> phases) {

    public record PhaseView(String name, String description, List<String> objectives, String status) {
    }
}
