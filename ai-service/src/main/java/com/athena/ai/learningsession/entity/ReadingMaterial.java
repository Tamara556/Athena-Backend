package com.athena.ai.learningsession.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "reading_materials")
@Getter
@Setter
@NoArgsConstructor
public class ReadingMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "text", nullable = false)
    private String content;

    @Column(name = "estimated_minutes", nullable = false)
    private int estimatedMinutes;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    public ReadingMaterial(UUID sessionId, String title, String content, int estimatedMinutes, int orderIndex) {
        this.sessionId = sessionId;
        this.title = title;
        this.content = content;
        this.estimatedMinutes = estimatedMinutes;
        this.orderIndex = orderIndex;
    }
}
