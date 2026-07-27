package com.athena.ai.onboarding.entity;

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
@Table(name = "assessments")
@Getter
@Setter
@NoArgsConstructor
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "questions_json", columnDefinition = "text")
    private String questionsJson;

    @Column(name = "answers_json", columnDefinition = "text")
    private String answersJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Assessment(UUID sessionId, UUID userId, String questionsJson) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.questionsJson = questionsJson;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
