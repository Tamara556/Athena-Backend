package com.athena.learning.service.impl;

import com.athena.common.event.LearningPlanCreatedEvent;
import com.athena.common.exception.ResourceNotFoundException;
import com.athena.learning.dto.CreatePlanRequest;
import com.athena.learning.dto.PlanResponse;
import com.athena.learning.entity.LearningPlan;
import com.athena.learning.mapper.LearningMapper;
import com.athena.learning.messaging.LearningEventPublisher;
import com.athena.learning.repository.LearningPlanRepository;
import com.athena.learning.service.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {

    private final LearningPlanRepository planRepository;
    private final LearningEventPublisher eventPublisher;

    @Override
    @Transactional
    public PlanResponse create(UUID userId, CreatePlanRequest request) {
        LearningPlan saved = planRepository.save(LearningMapper.toEntity(request, userId));
        log.info("Created plan planId={} userId={}", saved.getId(), userId);
        eventPublisher.publishPlanCreated(
                new LearningPlanCreatedEvent(saved.getId(), userId, saved.getTitle(), saved.getCreatedAt()));
        return LearningMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PlanResponse getById(UUID planId) {
        return planRepository.findById(planId)
                .map(LearningMapper::toResponse)
                .orElseThrow(() -> ResourceNotFoundException.of("Learning plan", planId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanResponse> getByUser(UUID userId) {
        return planRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(LearningMapper::toResponse)
                .toList();
    }
}
