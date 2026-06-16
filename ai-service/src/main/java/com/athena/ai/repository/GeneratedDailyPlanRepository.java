package com.athena.ai.repository;

import com.athena.ai.entity.GeneratedDailyPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GeneratedDailyPlanRepository extends JpaRepository<GeneratedDailyPlan, UUID> {

    Optional<GeneratedDailyPlan> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
}
