package com.athena.ai.dailyplan.service;

import com.athena.ai.dailyplan.dto.DailyPlanResponse;

import java.util.UUID;

public interface DailyPlanService {

    DailyPlanResponse getLatestForUser(UUID userId);
}
