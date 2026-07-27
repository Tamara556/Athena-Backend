package com.athena.rag.memory.repository;

import com.athena.rag.memory.domain.SourceType;
import com.athena.rag.memory.entity.MemoryDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemoryDocumentRepository extends JpaRepository<MemoryDocument, UUID> {

    Optional<MemoryDocument> findByUserIdAndSourceTypeAndEntityId(UUID userId, SourceType sourceType, UUID entityId);

    Optional<MemoryDocument> findByIdAndUserId(UUID id, UUID userId);

    List<MemoryDocument> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    List<MemoryDocument> findByUserIdAndSourceTypeOrderByUpdatedAtDesc(UUID userId, SourceType sourceType);

    long countByUserId(UUID userId);
}
