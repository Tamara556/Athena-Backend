package com.athena.ai.service;

import com.athena.ai.dto.DailyPlanResponse;

import java.util.UUID;

public interface DailyPlanService {

    DailyPlanResponse getLatestForUser(UUID userId);
}
