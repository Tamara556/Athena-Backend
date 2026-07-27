package com.athena.ai.dailyjourney.entity;

import com.athena.ai.dailyjourney.domain.DayStatus;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_missions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_mission_user_date", columnNames = {"user_id", "mission_date"})
})
@Getter
@Setter
@NoArgsConstructor
public class DailyMission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "mission_date", nullable = false)
    private LocalDate missionDate;

    @Column(name = "roadmap_id")
    private UUID roadmapId;

    @Column(name = "roadmap_node_id")
    private UUID roadmapNodeId;

    @Column(name = "learning_session_id")
    private UUID learningSessionId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "text", nullable = false)
    private String description;

    @Column(name = "goal_context", length = 300)
    private String goalContext;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty;

    @Column(name = "available_minutes", nullable = false)
    private int availableMinutes;

    @Column(name = "estimated_minutes", nullable = false)
    private int estimatedMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DayStatus status;

    @Column(name = "reasoning_json", columnDefinition = "text")
    private String reasoningJson;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DailyMission(UUID userId, LocalDate missionDate, UUID roadmapId, UUID roadmapNodeId,
                        UUID learningSessionId, String title, String description, String goalContext,
                        Difficulty difficulty, int availableMinutes, int estimatedMinutes) {
        this.userId = userId;
        this.missionDate = missionDate;
        this.roadmapId = roadmapId;
        this.roadmapNodeId = roadmapNodeId;
        this.learningSessionId = learningSessionId;
        this.title = title;
        this.description = description;
        this.goalContext = goalContext;
        this.difficulty = difficulty;
        this.availableMinutes = availableMinutes;
        this.estimatedMinutes = estimatedMinutes;
        this.status = DayStatus.READY;
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
