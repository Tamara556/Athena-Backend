package com.athena.learning.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "learning_session")
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

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_minutes")
    private long durationMinutes;

    public LearningSession(UUID userId, UUID taskId, Instant startedAt) {
        this.userId = userId;
        this.taskId = taskId;
        this.startedAt = startedAt;
    }

    public void end(Instant endedAt) {
        this.completedAt = endedAt;
        this.durationMinutes = Math.max(0, Duration.between(startedAt, endedAt).toMinutes());
    }

    public boolean isEnded() {
        return completedAt != null;
    }
}
