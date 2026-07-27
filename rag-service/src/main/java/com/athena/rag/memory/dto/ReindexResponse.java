package com.athena.rag.memory.dto;

import com.athena.rag.memory.service.ReindexOutcome;

public record ReindexResponse(int documentCount, int chunkCount) {

    public static ReindexResponse from(ReindexOutcome outcome) {
        return new ReindexResponse(outcome.documentCount(), outcome.chunkCount());
    }
}
