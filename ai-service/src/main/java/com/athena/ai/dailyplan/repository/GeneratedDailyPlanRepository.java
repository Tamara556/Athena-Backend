package com.athena.ai.dailyplan.repository;

import com.athena.ai.dailyplan.entity.GeneratedDailyPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GeneratedDailyPlanRepository extends JpaRepository<GeneratedDailyPlan, UUID> {

    Optional<GeneratedDailyPlan> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
}
