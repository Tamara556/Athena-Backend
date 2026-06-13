package com.athena.learning.mapper;

import com.athena.learning.dto.CreatePlanRequest;
import com.athena.learning.dto.CreateTaskRequest;
import com.athena.learning.dto.PlanResponse;
import com.athena.learning.dto.SessionResponse;
import com.athena.learning.dto.TaskResponse;
import com.athena.learning.entity.LearningPlan;
import com.athena.learning.entity.LearningSession;
import com.athena.learning.entity.LearningTask;

import java.util.UUID;

public final class LearningMapper {

    private LearningMapper() {
    }

    public static LearningPlan toEntity(CreatePlanRequest request, UUID userId) {
        LearningPlan plan = new LearningPlan();
        plan.setUserId(userId);
        plan.setTitle(request.title());
        plan.setDescription(request.description());
        return plan;
    }

    public static PlanResponse toResponse(LearningPlan plan) {
        return new PlanResponse(plan.getId(), plan.getUserId(), plan.getTitle(),
                plan.getDescription(), plan.getCreatedAt());
    }

    public static LearningTask toEntity(CreateTaskRequest request) {
        LearningTask task = new LearningTask();
        task.setPlanId(request.planId());
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setTaskType(request.taskType());
        task.setEstimatedMinutes(request.estimatedMinutes());
        return task;
    }

    public static TaskResponse toResponse(LearningTask task) {
        return new TaskResponse(task.getId(), task.getPlanId(), task.getTitle(), task.getDescription(),
                task.getTaskType(), task.getEstimatedMinutes(), task.getStatus(),
                task.getCompletedAt(), task.getCreatedAt());
    }

    public static SessionResponse toResponse(LearningSession session) {
        return new SessionResponse(session.getId(), session.getUserId(), session.getTaskId(),
                session.getStartedAt(), session.getCompletedAt(), session.getDurationMinutes());
    }
}
