package com.athena.rag.rag.service;

import com.athena.rag.config.RagProperties;
import com.athena.rag.memory.chunking.TokenEstimator;
import com.athena.rag.retrieval.domain.RetrievedChunk;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ContextAssemblerTest {

    private final TokenEstimator tokenEstimator = new TokenEstimator();

    private ContextAssembler assembler(int maxContextTokens) {
        return new ContextAssembler(new RagProperties(1024, 900, 150, 6, 0.35, maxContextTokens, 20), tokenEstimator);
    }

    @Test
    void returnsEmptyContextWhenNoChunks() {
        AssembledContext context = assembler(6000).assemble(List.of());
        assertThat(context.contextText()).isEmpty();
        assertThat(context.citations()).isEmpty();
        assertThat(context.usedCount()).isZero();
        assertThat(context.topScore()).isZero();
        assertThat(context.isEmpty()).isTrue();
    }

    @Test
    void numbersCitationsAndCarriesTopScoreFromFirstChunk() {
        AssembledContext context = assembler(6000).assemble(List.of(
                chunk("First body", "Intro", "math", 0.91),
                chunk("Second body", "Advanced", "math", 0.42)));

        assertThat(context.usedCount()).isEqualTo(2);
        assertThat(context.citations()).hasSize(2);
        assertThat(context.citations().getFirst().index()).isEqualTo(1);
        assertThat(context.citations().get(1).index()).isEqualTo(2);
        assertThat(context.contextText()).contains("[1]").contains("[2]");
        assertThat(context.topScore()).isEqualTo(0.91);
    }

    @Test
    void labelsChunkWithSourceTypeTitleAndDomain() {
        AssembledContext context = assembler(6000).assemble(List.of(
                chunk("body", "Fractions", "mathematics", 0.8)));
        assertThat(context.contextText()).contains("LESSON").contains("Fractions").contains("mathematics");
    }

    @Test
    void stopsAddingChunksOnceTokenBudgetExceededButAlwaysKeepsFirst() {
        String big = "word ".repeat(40); // ~200 chars -> ~50 tokens
        AssembledContext context = assembler(10).assemble(List.of(
                chunk(big, "One", "math", 0.9),
                chunk(big, "Two", "math", 0.5)));

        // First chunk is always included; the second exceeds the 10-token budget and is dropped.
        assertThat(context.usedCount()).isEqualTo(1);
        assertThat(context.citations()).hasSize(1);
    }

    private static RetrievedChunk chunk(String content, String title, String domain, double score) {
        return new RetrievedChunk(UUID.randomUUID(), UUID.randomUUID(), "LESSON", UUID.randomUUID(),
                domain, "core", title, content, score);
    }
}
