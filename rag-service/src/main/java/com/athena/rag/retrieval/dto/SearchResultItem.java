package com.athena.rag.retrieval.dto;

import com.athena.rag.retrieval.domain.RetrievedChunk;

import java.util.UUID;

public record SearchResultItem(
        UUID chunkId,
        UUID documentId,
        String sourceType,
        UUID entityId,
        String learningDomain,
        String category,
        String title,
        String snippet,
        double score
) {

    public static SearchResultItem from(RetrievedChunk chunk, int snippetLength) {
        String content = chunk.content() == null ? "" : chunk.content();
        String snippet = content.length() <= snippetLength ? content
                : content.substring(0, snippetLength).strip() + "…";
        return new SearchResultItem(chunk.chunkId(), chunk.documentId(), chunk.sourceType(), chunk.entityId(),
                chunk.learningDomain(), chunk.category(), chunk.title(), snippet, chunk.score());
    }
}
