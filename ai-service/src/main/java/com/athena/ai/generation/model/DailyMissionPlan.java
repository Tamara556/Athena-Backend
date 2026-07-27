package com.athena.ai.generation.model;

import java.util.List;

public record DailyMissionPlan(Mission mission, List<Block> blocks) {

    public record Mission(String title, String description, String goalContext, String difficulty) {
    }

    public record Block(String type, String title, String description, String difficulty, int durationMinutes) {
    }
}
