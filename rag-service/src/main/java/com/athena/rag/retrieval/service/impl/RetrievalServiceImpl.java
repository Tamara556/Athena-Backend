package com.athena.rag.retrieval.service.impl;

import com.athena.llm.EmbeddingProvider;
import com.athena.rag.memory.repository.ChunkVectorRepository;
import com.athena.rag.memory.repository.VectorMatch;
import com.athena.rag.memory.repository.VectorSearchQuery;
import com.athena.rag.observability.RagMetrics;
import com.athena.rag.retrieval.domain.RetrievedChunk;
import com.athena.rag.retrieval.service.RetrievalRequest;
import com.athena.rag.retrieval.service.RetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalServiceImpl implements RetrievalService {

    private final EmbeddingProvider embeddingProvider;
    private final ChunkVectorRepository vectorRepository;
    private final RagMetrics metrics;

    @Override
    public List<RetrievedChunk> retrieve(RetrievalRequest request) {
        if (request.query() == null || request.query().isBlank()) {
            throw new IllegalArgumentException("Query must not be blank");
        }
        long startNanos = System.nanoTime();
        try {
            float[] queryVector = embeddingProvider.embedOne(request.query());
            VectorSearchQuery vectorQuery = new VectorSearchQuery(
                    request.userId(), queryVector, request.sourceTypes(), request.learningDomain(),
                    request.includeGlobal(), request.topK(), Math.max(0, request.offset()));

            List<RetrievedChunk> matches = vectorRepository.search(vectorQuery).stream()
                    .filter(m -> m.score() >= request.minSimilarity())
                    .map(this::toRetrieved)
                    .toList();

            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
            double topScore = matches.isEmpty() ? 0.0 : matches.getFirst().score();
            metrics.recordRetrieval(latencyMs, matches.size(), topScore);
            log.info("retrieval userId={} matches={} topScore={} latencyMs={}",
                    request.userId(), matches.size(), topScore, latencyMs);
            return matches;
        } catch (RuntimeException ex) {
            metrics.retrievalFailure();
            log.error("retrieval failed userId={} cause={}", request.userId(), ex.getClass().getSimpleName());
            throw ex;
        }
    }

    private RetrievedChunk toRetrieved(VectorMatch match) {
        return new RetrievedChunk(match.chunkId(), match.documentId(), match.sourceType(), match.entityId(),
                match.learningDomain(), match.category(), match.title(), match.content(), match.score());
    }
}
