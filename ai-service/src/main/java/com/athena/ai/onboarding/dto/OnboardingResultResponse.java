package com.athena.ai.onboarding.dto;
import com.athena.ai.roadmap.dto.RoadmapResponse;
import com.athena.ai.dailyplan.dto.DailyPlanResponse;

import com.athena.ai.generation.model.GoalAnalysis;

public record OnboardingResultResponse(
        GoalAnalysis analysis,
        RoadmapResponse roadmap,
        DailyPlanResponse dailyPlan
) {
}
