package com.athena.learning.repository;

import com.athena.learning.domain.TaskStatus;
import com.athena.learning.entity.LearningTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LearningTaskRepository extends JpaRepository<LearningTask, UUID> {

    List<LearningTask> findByPlanIdOrderByCreatedAtAsc(UUID planId);

    long countByPlanIdAndStatusNot(UUID planId, TaskStatus status);
}
