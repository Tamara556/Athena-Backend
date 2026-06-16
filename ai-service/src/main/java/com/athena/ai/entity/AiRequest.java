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
import java.util.UUID;

@Entity
@Table(name = "ai_requests")
@Getter
@Setter
@NoArgsConstructor
public class AiRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "prompt_type", nullable = false, length = 60)
    private String promptType;

    @Column(nullable = false, length = 80)
    private String model;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AiRequest(UUID userId, String promptType, String model, String status) {
        this.userId = userId;
        this.promptType = promptType;
        this.model = model;
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
