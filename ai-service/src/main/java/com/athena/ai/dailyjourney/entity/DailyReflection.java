package com.athena.ai.dailyjourney.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_reflections", uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_reflection_user_date", columnNames = {"user_id", "reflection_date"})
})
@Getter
@Setter
@NoArgsConstructor
public class DailyReflection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "mission_id", nullable = false)
    private UUID missionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "reflection_date", nullable = false)
    private LocalDate reflectionDate;

    @Column(name = "hardest_part", columnDefinition = "text")
    private String hardestPart;

    @Column(name = "what_clicked", columnDefinition = "text")
    private String whatClicked;

    @Column(name = "adjust_request", columnDefinition = "text")
    private String adjustRequest;

    @Column(nullable = false)
    private boolean skipped;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public DailyReflection(UUID missionId, UUID userId, LocalDate reflectionDate, String hardestPart,
                           String whatClicked, String adjustRequest, boolean skipped) {
        this.missionId = missionId;
        this.userId = userId;
        this.reflectionDate = reflectionDate;
        this.hardestPart = hardestPart;
        this.whatClicked = whatClicked;
        this.adjustRequest = adjustRequest;
        this.skipped = skipped;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
