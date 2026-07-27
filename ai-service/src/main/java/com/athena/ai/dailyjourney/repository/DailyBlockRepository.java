package com.athena.ai.dailyjourney.repository;

import com.athena.ai.dailyjourney.domain.BlockStatus;
import com.athena.ai.dailyjourney.domain.BlockType;
import com.athena.ai.dailyjourney.entity.DailyBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyBlockRepository extends JpaRepository<DailyBlock, UUID> {

    List<DailyBlock> findByMissionIdOrderByOrderIndexAsc(UUID missionId);

    Optional<DailyBlock> findByIdAndUserId(UUID id, UUID userId);

    long countByUserIdAndTypeAndStatus(UUID userId, BlockType type, BlockStatus status);
}
