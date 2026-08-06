package com.athena.rag.memory.chunking;

import com.athena.rag.config.RagProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkerTest {

    // chunkMaxTokens=13 -> wordsPerChunk = floor(13/1.3)=10; overlap tokens=3 -> overlapWords=floor(3/1.3)=2; step=8.
    private final RagProperties properties = new RagProperties(1024, 13, 3, 6, 0.35, 6000, 20);
    private final TextChunker chunker = new TextChunker(properties, new TokenEstimator());

    @Test
    void returnsEmptyForNull() {
        assertThat(chunker.chunk(null)).isEmpty();
    }

    @Test
    void returnsEmptyForBlank() {
        assertThat(chunker.chunk("   \n  ")).isEmpty();
    }

    @Test
    void keepsShortTextInSingleChunk() {
        List<Chunk> chunks = chunker.chunk("just a few words here");
        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().index()).isZero();
        assertThat(chunks.getFirst().content()).isEqualTo("just a few words here");
        assertThat(chunks.getFirst().tokenCount()).isPositive();
    }

    @Test
    void splitsLongTextIntoOverlappingChunksWithSequentialIndices() {
        String text = IntStream.rangeClosed(1, 20).mapToObj(i -> "w" + i)
                .reduce((a, b) -> a + " " + b).orElseThrow();

        List<Chunk> chunks = chunker.chunk(text);

        // 20 words, step 8: windows [0,10) [8,18) [16,20) -> 3 chunks.
        assertThat(chunks).hasSize(3);
        assertThat(chunks).extracting(Chunk::index).containsExactly(0, 1, 2);
        // Overlap: second chunk starts at word index 8 (w9).
        assertThat(chunks.get(1).content()).startsWith("w9 w10");
        // Last chunk holds the tail and is not empty.
        assertThat(chunks.get(2).content()).endsWith("w20");
    }

    @Test
    void everyChunkHasEstimatedTokenCount() {
        List<Chunk> chunks = chunker.chunk("alpha beta gamma delta epsilon zeta eta theta iota kappa lambda");
        assertThat(chunks).isNotEmpty();
        assertThat(chunks).allSatisfy(c -> assertThat(c.tokenCount()).isPositive());
    }
}
