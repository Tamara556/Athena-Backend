package com.athena.ai.dailyjourney.entity;

import com.athena.ai.dailyjourney.domain.ConfidenceLevel;
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
@Table(name = "daily_checkins")
@Getter
@Setter
@NoArgsConstructor
public class DailyCheckin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "mission_id", nullable = false)
    private UUID missionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "block_id")
    private UUID blockId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConfidenceLevel confidence;

    @Column(length = 300)
    private String topic;

    @Column(columnDefinition = "text", nullable = false)
    private String reply;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public DailyCheckin(UUID missionId, UUID userId, UUID blockId, ConfidenceLevel confidence,
                        String topic, String reply) {
        this.missionId = missionId;
        this.userId = userId;
        this.blockId = blockId;
        this.confidence = confidence;
        this.topic = topic;
        this.reply = reply;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
