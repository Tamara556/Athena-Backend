package com.athena.ai.roadmap.repository;

import com.athena.ai.roadmap.entity.GeneratedRoadmap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GeneratedRoadmapRepository extends JpaRepository<GeneratedRoadmap, UUID> {

    Optional<GeneratedRoadmap> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
}
