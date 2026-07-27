package com.athena.ai.knowledgegraph.repository;

import com.athena.ai.knowledgegraph.entity.KnowledgeGraphSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgeGraphSnapshotRepository extends JpaRepository<KnowledgeGraphSnapshot, UUID> {

    List<KnowledgeGraphSnapshot> findByUserIdOrderByGeneratedAtDesc(UUID userId);

    Optional<KnowledgeGraphSnapshot> findFirstByUserIdOrderByGeneratedAtDesc(UUID userId);
}
