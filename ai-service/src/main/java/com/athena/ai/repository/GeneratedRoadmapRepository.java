package com.athena.ai.repository;

import com.athena.ai.entity.GeneratedRoadmap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GeneratedRoadmapRepository extends JpaRepository<GeneratedRoadmap, UUID> {

    Optional<GeneratedRoadmap> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
}
