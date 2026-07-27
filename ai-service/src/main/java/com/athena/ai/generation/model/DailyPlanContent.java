package com.athena.ai.generation.model;

import java.util.List;

public record DailyPlanContent(List<PlanItem> items) {

    public record PlanItem(String type, String title, String description, int estimatedMinutes) {
    }
}
