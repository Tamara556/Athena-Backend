package com.athena.rag.rag.service;

import com.athena.rag.config.RagProperties;
import com.athena.rag.memory.chunking.TokenEstimator;
import com.athena.rag.rag.dto.Citation;
import com.athena.rag.retrieval.domain.RetrievedChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ContextAssembler {

    private final RagProperties properties;
    private final TokenEstimator tokenEstimator;

    public ContextAssembler(RagProperties properties, TokenEstimator tokenEstimator) {
        this.properties = properties;
        this.tokenEstimator = tokenEstimator;
    }

    public AssembledContext assemble(List<RetrievedChunk> chunks) {
        if (chunks.isEmpty()) {
            return new AssembledContext("", List.of(), 0, 0.0);
        }

        StringBuilder context = new StringBuilder();
        List<Citation> citations = new ArrayList<>();
        int budget = properties.maxContextTokens();
        int used = 0;
        int index = 1;

        for (RetrievedChunk chunk : chunks) {
            int tokens = tokenEstimator.estimate(chunk.content());
            if (used > 0 && used + tokens > budget) {
                break;
            }
            String label = describe(chunk);
            context.append('[').append(index).append("] ").append(label).append('\n')
                    .append(chunk.content()).append("\n\n");
            citations.add(new Citation(index, chunk.sourceType(), chunk.entityId(), chunk.title(), chunk.score()));
            used += tokens;
            index++;
        }

        return new AssembledContext(context.toString().strip(), citations,
                citations.size(), chunks.getFirst().score());
    }

    private String describe(RetrievedChunk chunk) {
        StringBuilder label = new StringBuilder(chunk.sourceType());
        if (chunk.title() != null && !chunk.title().isBlank()) {
            label.append(" — ").append(chunk.title());
        }
        if (chunk.learningDomain() != null && !chunk.learningDomain().isBlank()) {
            label.append(" (").append(chunk.learningDomain()).append(')');
        }
        return label.toString();
    }
}
