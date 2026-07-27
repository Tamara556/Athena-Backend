package com.athena.ai.dailyjourney.entity;

import com.athena.ai.dailyjourney.domain.AdjustmentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "adjustment_logs")
@Getter
@Setter
@NoArgsConstructor
public class AdjustmentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "mission_id", nullable = false)
    private UUID missionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdjustmentType type;

    @Column(nullable = false, length = 300)
    private String reason;

    @Column(name = "affected_block_id")
    private UUID affectedBlockId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AdjustmentLog(UUID missionId, UUID userId, AdjustmentType type, String reason, UUID affectedBlockId) {
        this.missionId = missionId;
        this.userId = userId;
        this.type = type;
        this.reason = reason;
        this.affectedBlockId = affectedBlockId;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
