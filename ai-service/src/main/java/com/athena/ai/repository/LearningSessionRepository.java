package com.athena.ai.repository;

import com.athena.ai.entity.LearningSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearningSessionRepository extends JpaRepository<LearningSession, UUID> {

    Optional<LearningSession> findByIdAndUserId(UUID id, UUID userId);

    Optional<LearningSession> findByUserIdAndRoadmapNodeId(UUID userId, UUID roadmapNodeId);

    boolean existsByUserIdAndRoadmapNodeId(UUID userId, UUID roadmapNodeId);

    List<LearningSession> findByUserIdOrderByNodeIndexAsc(UUID userId);

    List<LearningSession> findByUserIdAndRoadmapIdOrderByNodeIndexAsc(UUID userId, UUID roadmapId);
}
