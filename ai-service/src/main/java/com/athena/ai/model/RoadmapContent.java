package com.athena.ai.model;

import java.util.List;

public record RoadmapContent(List<Phase> phases) {

    public record Phase(String name, String description, int durationWeeks, List<String> objectives, String status) {
    }

}
