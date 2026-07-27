package com.athena.ai.dailyjourney.entity;

import com.athena.ai.dailyjourney.domain.BlockStatus;
import com.athena.ai.dailyjourney.domain.BlockType;
import com.athena.ai.learningsession.domain.Difficulty;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "daily_blocks")
@Getter
@Setter
@NoArgsConstructor
public class DailyBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "mission_id", nullable = false)
    private UUID missionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BlockType type;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "roadmap_node_id")
    private UUID roadmapNodeId;

    @Column(name = "knowledge_node_id")
    private UUID knowledgeNodeId;

    @Column(name = "source_ref")
    private UUID sourceRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BlockStatus status;

    @Column(name = "progress_percent", nullable = false)
    private int progressPercent;

    @Column(name = "skip_reason", length = 300)
    private String skipReason;

    @Column(name = "priority_insert", nullable = false)
    private boolean priorityInsert;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DailyBlock(UUID missionId, UUID userId, int orderIndex, BlockType type, String title,
                      String description, UUID roadmapNodeId, UUID knowledgeNodeId, UUID sourceRef,
                      Difficulty difficulty, int durationMinutes, BlockStatus status, boolean priorityInsert) {
        this.missionId = missionId;
        this.userId = userId;
        this.orderIndex = orderIndex;
        this.type = type;
        this.title = title;
        this.description = description;
        this.roadmapNodeId = roadmapNodeId;
        this.knowledgeNodeId = knowledgeNodeId;
        this.sourceRef = sourceRef;
        this.difficulty = difficulty;
        this.durationMinutes = durationMinutes;
        this.status = status;
        this.priorityInsert = priorityInsert;
        this.progressPercent = status == BlockStatus.COMPLETED ? 100 : 0;
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
