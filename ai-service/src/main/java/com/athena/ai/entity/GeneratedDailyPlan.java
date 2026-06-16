package com.athena.ai.entity;

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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "generated_daily_plans")
@Getter
@Setter
@NoArgsConstructor
public class GeneratedDailyPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "roadmap_id")
    private UUID roadmapId;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @Column(name = "content_json", columnDefinition = "text", nullable = false)
    private String contentJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public GeneratedDailyPlan(UUID userId, UUID roadmapId, LocalDate planDate, String contentJson) {
        this.userId = userId;
        this.roadmapId = roadmapId;
        this.planDate = planDate;
        this.contentJson = contentJson;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
