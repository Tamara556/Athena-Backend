package com.athena.ai.dailyjourney.repository;

import com.athena.ai.dailyjourney.entity.DailyMission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface DailyMissionRepository extends JpaRepository<DailyMission, UUID> {

    Optional<DailyMission> findByUserIdAndMissionDate(UUID userId, LocalDate missionDate);

    Optional<DailyMission> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndMissionDate(UUID userId, LocalDate missionDate);
}
