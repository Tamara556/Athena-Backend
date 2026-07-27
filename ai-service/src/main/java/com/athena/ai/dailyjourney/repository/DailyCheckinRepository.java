package com.athena.ai.dailyjourney.repository;

import com.athena.ai.dailyjourney.entity.DailyCheckin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DailyCheckinRepository extends JpaRepository<DailyCheckin, UUID> {

    Optional<DailyCheckin> findFirstByMissionIdOrderByCreatedAtDesc(UUID missionId);
}
