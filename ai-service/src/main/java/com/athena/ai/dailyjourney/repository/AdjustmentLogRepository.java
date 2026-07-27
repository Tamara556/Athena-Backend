package com.athena.ai.dailyjourney.repository;

import com.athena.ai.dailyjourney.entity.AdjustmentLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdjustmentLogRepository extends JpaRepository<AdjustmentLog, UUID> {

    List<AdjustmentLog> findByMissionIdOrderByCreatedAtDesc(UUID missionId);
}
