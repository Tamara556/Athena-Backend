package com.athena.rag.memory.repository;

import com.athena.rag.memory.entity.MemoryChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MemoryChunkRepository extends JpaRepository<MemoryChunk, UUID> {

    List<MemoryChunk> findByDocumentIdOrderByChunkIndexAsc(UUID documentId);
}
