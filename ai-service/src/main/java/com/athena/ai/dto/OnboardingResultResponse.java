package com.athena.ai.dto;

import com.athena.ai.model.GoalAnalysis;

public record OnboardingResultResponse(
        GoalAnalysis analysis,
        RoadmapResponse roadmap,
        DailyPlanResponse dailyPlan
) {
}
