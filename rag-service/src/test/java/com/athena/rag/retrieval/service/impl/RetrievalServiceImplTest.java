package com.athena.rag.retrieval.service.impl;

import com.athena.llm.EmbeddingProvider;
import com.athena.rag.memory.repository.ChunkVectorRepository;
import com.athena.rag.memory.repository.VectorMatch;
import com.athena.rag.observability.RagMetrics;
import com.athena.rag.retrieval.domain.RetrievedChunk;
import com.athena.rag.retrieval.service.RetrievalRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetrievalServiceImplTest {

    @Mock
    private EmbeddingProvider embeddingProvider;
    @Mock
    private ChunkVectorRepository vectorRepository;
    @Mock
    private RagMetrics metrics;

    @InjectMocks
    private RetrievalServiceImpl service;

    private final UUID userId = UUID.randomUUID();

    @Test
    void rejectsBlankQueryWithoutEmbedding() {
        assertThatThrownBy(() -> service.retrieve(request("   ", 0.35)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(embeddingProvider, vectorRepository);
    }

    @Test
    void filtersMatchesBelowMinimumSimilarityAndMaps() {
        when(embeddingProvider.embedOne("what next")).thenReturn(new float[]{0.1f, 0.2f});
        when(vectorRepository.search(any())).thenReturn(List.of(match(0.90), match(0.20)));

        List<RetrievedChunk> result = service.retrieve(request("what next", 0.35));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().score()).isEqualTo(0.90);
        assertThat(result.getFirst().sourceType()).isEqualTo("LESSON");
        verify(metrics).recordRetrieval(anyLong(), eq(1), eq(0.90));
    }

    @Test
    void recordsFailureAndRethrowsWhenSearchFails() {
        when(embeddingProvider.embedOne("boom")).thenReturn(new float[]{0.1f});
        when(vectorRepository.search(any())).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.retrieve(request("boom", 0.35)))
                .isInstanceOf(IllegalStateException.class);

        verify(metrics).retrievalFailure();
        verify(metrics, never()).recordRetrieval(anyLong(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyDouble());
    }

    private RetrievalRequest request(String query, double minSimilarity) {
        return new RetrievalRequest(userId, query, null, null, 6, minSimilarity, true, 0);
    }

    private VectorMatch match(double score) {
        return new VectorMatch(UUID.randomUUID(), UUID.randomUUID(), userId, "LESSON", UUID.randomUUID(),
                "math", "core", "Title", "content", score);
    }
}
