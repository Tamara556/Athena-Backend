package com.athena.ai.entity;

import com.athena.ai.domain.SessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "learning_sessions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_learning_session_node", columnNames = {"user_id", "roadmap_node_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class LearningSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "roadmap_id", nullable = false)
    private UUID roadmapId;

    @Column(name = "roadmap_node_id", nullable = false)
    private UUID roadmapNodeId;

    @Column(name = "node_index", nullable = false)
    private int nodeIndex;

    @Column(nullable = false, length = 300)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "estimated_minutes", nullable = false)
    private int estimatedMinutes;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public LearningSession(UUID userId, UUID roadmapId, UUID roadmapNodeId, int nodeIndex,
                           String title, int estimatedMinutes) {
        this.userId = userId;
        this.roadmapId = roadmapId;
        this.roadmapNodeId = roadmapNodeId;
        this.nodeIndex = nodeIndex;
        this.title = title;
        this.estimatedMinutes = estimatedMinutes;
        this.status = SessionStatus.NOT_STARTED;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.generatedAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
