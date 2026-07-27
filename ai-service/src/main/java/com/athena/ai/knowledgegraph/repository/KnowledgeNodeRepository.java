package com.athena.ai.knowledgegraph.repository;

import com.athena.ai.knowledgegraph.entity.KnowledgeNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgeNodeRepository extends JpaRepository<KnowledgeNode, UUID> {

    List<KnowledgeNode> findByUserIdOrderBySkillNameAsc(UUID userId);

    Optional<KnowledgeNode> findByUserIdAndSkillNameIgnoreCase(UUID userId, String skillName);
}
