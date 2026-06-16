package com.athena.ai.repository;

import com.athena.ai.entity.KnowledgeNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgeNodeRepository extends JpaRepository<KnowledgeNode, UUID> {

    List<KnowledgeNode> findByUserIdOrderBySkillNameAsc(UUID userId);

    Optional<KnowledgeNode> findByUserIdAndSkillNameIgnoreCase(UUID userId, String skillName);
}
