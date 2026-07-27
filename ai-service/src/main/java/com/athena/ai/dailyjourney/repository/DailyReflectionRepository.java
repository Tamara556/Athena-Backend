package com.athena.ai.dailyjourney.repository;

import com.athena.ai.dailyjourney.entity.DailyReflection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DailyReflectionRepository extends JpaRepository<DailyReflection, UUID> {

    Optional<DailyReflection> findByMissionId(UUID missionId);

    Optional<DailyReflection> findFirstByUserIdOrderByReflectionDateDesc(UUID userId);

}
