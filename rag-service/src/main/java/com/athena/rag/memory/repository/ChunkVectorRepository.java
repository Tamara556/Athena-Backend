package com.athena.rag.memory.repository;

import java.util.List;
import java.util.UUID;

public interface ChunkVectorRepository {

    void insertBatch(List<ChunkInsert> chunks);

    void deleteByDocumentId(UUID documentId);

    List<VectorMatch> search(VectorSearchQuery query);
}
