package com.athena.rag.rag.service;

import com.athena.rag.config.RagProperties;
import com.athena.rag.retrieval.domain.RetrievedChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GroundingPolicyTest {

    // minSimilarity = 0.35
    private final RagProperties properties = new RagProperties(1024, 900, 150, 6, 0.35, 6000, 20);
    private final GroundingPolicy policy = new GroundingPolicy(properties);

    @Test
    void notGroundedWhenNoChunks() {
        assertThat(policy.isGrounded(List.of())).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "0.10, false",
            "0.34, false",
            "0.35, true",
            "0.90, true"
    })
    void groundedOnlyWhenTopScoreMeetsThreshold(double topScore, boolean expected) {
        assertThat(policy.isGrounded(List.of(chunk(topScore)))).isEqualTo(expected);
    }

    @Test
    void usesFirstChunkScoreAsTheDecision() {
        // First chunk is above threshold even though the second is below.
        assertThat(policy.isGrounded(List.of(chunk(0.80), chunk(0.10)))).isTrue();
    }

    private static RetrievedChunk chunk(double score) {
        return new RetrievedChunk(UUID.randomUUID(), UUID.randomUUID(), "LESSON", UUID.randomUUID(),
                "math", "core", "Intro", "content", score);
    }
}
