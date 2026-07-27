package com.athena.ai.dailyplan.dto;

import com.athena.ai.generation.model.DailyPlanContent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DailyPlanResponse(
        UUID id,
        LocalDate date,
        List<DailyPlanContent.PlanItem> items,
        Instant createdAt
) {
}
