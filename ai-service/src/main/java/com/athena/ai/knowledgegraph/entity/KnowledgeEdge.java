package com.athena.ai.knowledgegraph.entity;

import com.athena.ai.knowledgegraph.domain.RelationshipType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_edges", uniqueConstraints =
        @UniqueConstraint(name = "uk_knowledge_edge",
                columnNames = {"user_id", "source_skill", "target_skill", "relationship_type"}))
@Getter
@Setter
@NoArgsConstructor
public class KnowledgeEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "source_skill", nullable = false, length = 120)
    private String sourceSkill;

    @Column(name = "target_skill", nullable = false, length = 120)
    private String targetSkill;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false, length = 20)
    private RelationshipType relationshipType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public KnowledgeEdge(UUID userId, String sourceSkill, String targetSkill, RelationshipType relationshipType) {
        this.userId = userId;
        this.sourceSkill = sourceSkill;
        this.targetSkill = targetSkill;
        this.relationshipType = relationshipType;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
