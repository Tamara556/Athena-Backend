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
@Table(name = "generated_roadmaps")
@Getter
@Setter
@NoArgsConstructor
public class GeneratedRoadmap {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(nullable = false, length = 1000)
    private String goal;

    @Column(length = 40)
    private String level;

    @Column(name = "content_json", columnDefinition = "text", nullable = false)
    private String contentJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public GeneratedRoadmap(UUID userId, UUID sessionId, String goal, String level, String contentJson) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.goal = goal;
        this.level = level;
        this.contentJson = contentJson;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
