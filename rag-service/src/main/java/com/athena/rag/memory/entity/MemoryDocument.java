package com.athena.rag.memory.entity;

import com.athena.rag.memory.domain.DocumentStatus;
import com.athena.rag.memory.domain.SourceType;
import com.athena.rag.memory.domain.Visibility;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "memory_document", uniqueConstraints =
        @UniqueConstraint(name = "uk_memory_document_source", columnNames = {"user_id", "source_type", "entity_id"}))
@Getter
@Setter
@NoArgsConstructor
public class MemoryDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40, updatable = false)
    private SourceType sourceType;

    @Column(name = "entity_id", updatable = false)
    private UUID entityId;

    @Column(name = "learning_domain", length = 120)
    private String learningDomain;

    @Column(length = 60)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibility visibility;

    @Column(length = 300)
    private String title;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public MemoryDocument(UUID userId, SourceType sourceType, UUID entityId, String learningDomain,
                          String category, Visibility visibility, String title, String contentHash) {
        this.userId = userId;
        this.sourceType = sourceType;
        this.entityId = entityId;
        this.learningDomain = learningDomain;
        this.category = category;
        this.visibility = visibility;
        this.title = title;
        this.contentHash = contentHash;
        this.status = DocumentStatus.INDEXED;
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
