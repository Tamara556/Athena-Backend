package com.athena.rag.rag.service;

import com.athena.rag.config.RagProperties;
import com.athena.rag.retrieval.domain.RetrievedChunk;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GroundingPolicy {

    private final RagProperties properties;

    public GroundingPolicy(RagProperties properties) {
        this.properties = properties;
    }

    public boolean isGrounded(List<RetrievedChunk> chunks) {
        return !chunks.isEmpty() && chunks.getFirst().score() >= properties.minSimilarity();
    }
}
