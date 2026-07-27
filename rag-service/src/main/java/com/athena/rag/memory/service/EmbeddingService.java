package com.athena.rag.memory.service;

import java.util.UUID;

public interface EmbeddingService {

    IngestOutcome ingest(MemoryIngestCommand command);

    void delete(UUID userId, UUID documentId);

    ReindexOutcome reindexUser(UUID userId);
}
