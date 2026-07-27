package com.athena.rag.rag.entity;

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
@Table(name = "rag_query_log")
@Getter
@Setter
@NoArgsConstructor
public class RagQueryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "query_text", nullable = false, updatable = false)
    private String queryText;

    @Column(name = "retrieved_count", nullable = false)
    private int retrievedCount;

    @Column(name = "top_score")
    private Double topScore;

    @Column(nullable = false)
    private boolean grounded;

    @Column(length = 120)
    private String model;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    @Column(name = "prompt_tokens", nullable = false)
    private int promptTokens;

    @Column(name = "completion_tokens", nullable = false)
    private int completionTokens;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public RagQueryLog(UUID userId, String queryText, int retrievedCount, Double topScore, boolean grounded,
                       String model, long latencyMs, int promptTokens, int completionTokens, String status) {
        this.userId = userId;
        this.queryText = queryText;
        this.retrievedCount = retrievedCount;
        this.topScore = topScore;
        this.grounded = grounded;
        this.model = model;
        this.latencyMs = latencyMs;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
