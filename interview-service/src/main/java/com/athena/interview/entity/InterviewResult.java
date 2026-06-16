package com.athena.interview.entity;

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
@Table(name = "interview_results")
@Getter
@Setter
@NoArgsConstructor
public class InterviewResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "interview_id", nullable = false)
    private UUID interviewId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private boolean passed;

    @Column(name = "weaknesses_json", columnDefinition = "text")
    private String weaknessesJson;

    @Column(name = "recommendations_json", columnDefinition = "text")
    private String recommendationsJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public InterviewResult(UUID interviewId, UUID userId, int score, boolean passed,
                           String weaknessesJson, String recommendationsJson) {
        this.interviewId = interviewId;
        this.userId = userId;
        this.score = score;
        this.passed = passed;
        this.weaknessesJson = weaknessesJson;
        this.recommendationsJson = recommendationsJson;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
