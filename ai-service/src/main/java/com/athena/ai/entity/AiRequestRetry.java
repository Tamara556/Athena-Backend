package com.athena.ai.entity;

import com.athena.ai.domain.RetryStatus;
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
@Table(name = "ai_request_retries")
@Getter
@Setter
@NoArgsConstructor
public class AiRequestRetry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "request_type", nullable = false, length = 60)
    private String requestType;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "payload_reference", nullable = false)
    private String payloadReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RetryStatus status = RetryStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AiRequestRetry(String requestType, UUID userId, String payloadReference) {
        this.requestType = requestType;
        this.userId = userId;
        this.payloadReference = payloadReference;
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
