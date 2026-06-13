package com.athena.learning.service;

import com.athena.learning.dto.CreatePlanRequest;
import com.athena.learning.dto.PlanResponse;

import java.util.List;
import java.util.UUID;

public interface PlanService {

    PlanResponse create(UUID userId, CreatePlanRequest request);

    PlanResponse getById(UUID planId);

    List<PlanResponse> getByUser(UUID userId);
}
