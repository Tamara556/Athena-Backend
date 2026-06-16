package com.athena.ai.repository;

import com.athena.ai.domain.RelationshipType;
import com.athena.ai.entity.KnowledgeEdge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KnowledgeEdgeRepository extends JpaRepository<KnowledgeEdge, UUID> {

    List<KnowledgeEdge> findByUserId(UUID userId);

    boolean existsByUserIdAndSourceSkillAndTargetSkillAndRelationshipType(
            UUID userId, String sourceSkill, String targetSkill, RelationshipType relationshipType);
}
