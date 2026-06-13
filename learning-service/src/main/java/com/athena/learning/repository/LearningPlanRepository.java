package com.athena.learning.repository;

import com.athena.learning.entity.LearningPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LearningPlanRepository extends JpaRepository<LearningPlan, UUID> {

    List<LearningPlan> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
