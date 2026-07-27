package com.athena.ai.learningsession.entity;

import com.athena.ai.learningsession.domain.PracticeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "practice_activities")
@Getter
@Setter
@NoArgsConstructor
public class PracticeActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "text", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "practice_type", nullable = false, length = 30)
    private PracticeType practiceType;

    @Column(columnDefinition = "text", nullable = false)
    private String instructions;

    @Column(name = "starter_content", columnDefinition = "text")
    private String starterContent;

    @Column(name = "estimated_minutes", nullable = false)
    private int estimatedMinutes;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    public PracticeActivity(UUID sessionId, String title, String description, PracticeType practiceType,
                            String instructions, String starterContent, int estimatedMinutes, int orderIndex) {
        this.sessionId = sessionId;
        this.title = title;
        this.description = description;
        this.practiceType = practiceType;
        this.instructions = instructions;
        this.starterContent = starterContent;
        this.estimatedMinutes = estimatedMinutes;
        this.orderIndex = orderIndex;
    }
}
