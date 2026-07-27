package com.athena.ai.generation.entity;

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
@Table(name = "ai_responses")
@Getter
@Setter
@NoArgsConstructor
public class AiResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    @Column(name = "total_tokens", nullable = false)
    private int totalTokens;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "error_type", length = 120)
    private String errorType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AiResponse(UUID requestId, long latencyMs, int totalTokens, boolean success, String errorType) {
        this.requestId = requestId;
        this.latencyMs = latencyMs;
        this.totalTokens = totalTokens;
        this.success = success;
        this.errorType = errorType;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
