package com.athena.ai.knowledgegraph.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_nodes", uniqueConstraints =
        @UniqueConstraint(name = "uk_knowledge_user_skill", columnNames = {"user_id", "skill_name"}))
@Getter
@Setter
@NoArgsConstructor
public class KnowledgeNode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "skill_name", nullable = false, length = 120)
    private String skillName;

    @Column(length = 120)
    private String domain;

    @Column(length = 40, nullable = false)
    private String category = "CORE";

    @Column(name = "mastery_percentage", nullable = false)
    private int masteryPercentage;

    @Column(name = "confidence_score", nullable = false)
    private double confidenceScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public KnowledgeNode(UUID userId, String skillName, String domain, int masteryPercentage, double confidenceScore) {
        this.userId = userId;
        this.skillName = skillName;
        this.domain = domain;
        this.masteryPercentage = masteryPercentage;
        this.confidenceScore = confidenceScore;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
