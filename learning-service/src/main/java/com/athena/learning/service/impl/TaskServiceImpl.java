package com.athena.learning.service.impl;

import com.athena.common.event.TaskCompletedEvent;
import com.athena.common.exception.ResourceNotFoundException;
import com.athena.learning.constants.LearningConstants;
import com.athena.learning.domain.TaskStatus;
import com.athena.learning.dto.CreateTaskRequest;
import com.athena.learning.dto.TaskResponse;
import com.athena.learning.entity.LearningPlan;
import com.athena.learning.entity.LearningTask;
import com.athena.learning.mapper.LearningMapper;
import com.athena.learning.messaging.LearningEventPublisher;
import com.athena.learning.repository.LearningPlanRepository;
import com.athena.learning.repository.LearningTaskRepository;
import com.athena.learning.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final LearningTaskRepository taskRepository;
    private final LearningPlanRepository planRepository;
    private final LearningEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    @Transactional
    public TaskResponse create(CreateTaskRequest request) {
        if (!planRepository.existsById(request.planId())) {
            throw ResourceNotFoundException.of(LearningConstants.LEARNING_PLAN_RESOURCE, request.planId());
        }
        LearningTask saved = taskRepository.save(LearningMapper.toEntity(request));
        log.info("Created task taskId={} planId={}", saved.getId(), saved.getPlanId());
        return LearningMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getById(UUID taskId) {
        return taskRepository.findById(taskId)
                .map(LearningMapper::toResponse)
                .orElseThrow(() -> ResourceNotFoundException.of(LearningConstants.LEARNING_TASK_RESOURCE, taskId));
    }

    @Override
    @Transactional
    public TaskResponse complete(UUID taskId) {
        LearningTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> ResourceNotFoundException.of(LearningConstants.LEARNING_TASK_RESOURCE, taskId));

        if (task.isCompleted()) {
            log.info("Task taskId={} already completed; skipping re-publish", taskId);
            return LearningMapper.toResponse(task);
        }

        LearningPlan plan = planRepository.findById(task.getPlanId())
                .orElseThrow(() -> ResourceNotFoundException.of(LearningConstants.LEARNING_PLAN_RESOURCE, task.getPlanId()));

        Instant now = clock.instant();
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(now);
        LearningTask saved = taskRepository.save(task);

        eventPublisher.publishTaskCompleted(new TaskCompletedEvent(
                plan.getUserId(), saved.getId(), saved.getPlanId(),
                saved.getTaskType().name(), saved.getEstimatedMinutes(), now));
        log.info("Completed task taskId={} userId={}", taskId, plan.getUserId());

        return LearningMapper.toResponse(saved);
    }
}
