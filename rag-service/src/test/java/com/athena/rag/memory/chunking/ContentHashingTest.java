package com.athena.rag.memory.chunking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContentHashingTest {

    @Test
    void isDeterministicForSameInput() {
        assertThat(ContentHashing.sha256("the quick brown fox"))
                .isEqualTo(ContentHashing.sha256("the quick brown fox"));
    }

    @Test
    void differsForDifferentInput() {
        assertThat(ContentHashing.sha256("alpha")).isNotEqualTo(ContentHashing.sha256("beta"));
    }

    @Test
    void producesSixtyFourHexCharacters() {
        assertThat(ContentHashing.sha256("anything")).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    void matchesKnownSha256Vector() {
        assertThat(ContentHashing.sha256(""))
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }
}
