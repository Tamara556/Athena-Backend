package com.athena.ai.dailyplan.service.impl;

import com.athena.ai.constants.AiConstants;
import com.athena.ai.dailyplan.dto.DailyPlanResponse;
import com.athena.ai.dailyplan.entity.GeneratedDailyPlan;
import com.athena.ai.generation.model.DailyPlanContent;
import com.athena.ai.dailyplan.repository.GeneratedDailyPlanRepository;
import com.athena.ai.dailyplan.service.DailyPlanService;
import com.athena.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DailyPlanServiceImpl implements DailyPlanService {

    private final GeneratedDailyPlanRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public DailyPlanResponse getLatestForUser(UUID userId) {
        GeneratedDailyPlan plan = repository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> ResourceNotFoundException.of(AiConstants.RESOURCE_DAILY_PLAN, userId));
        DailyPlanContent content = objectMapper.readValue(plan.getContentJson(), DailyPlanContent.class);
        return new DailyPlanResponse(plan.getId(), plan.getPlanDate(), content.items(), plan.getCreatedAt());
    }
}
