package com.athena.ai.knowledgegraph.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_graph_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class KnowledgeGraphSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    @Column(name = "average_mastery", nullable = false)
    private int averageMastery;

    @Column(name = "serialized_graph", columnDefinition = "text", nullable = false)
    private String serializedGraph;

    public KnowledgeGraphSnapshot(UUID userId, int averageMastery, String serializedGraph) {
        this.userId = userId;
        this.averageMastery = averageMastery;
        this.serializedGraph = serializedGraph;
    }

    @PrePersist
    void onCreate() {
        if (generatedAt == null) {
            generatedAt = Instant.now();
        }
    }
}
