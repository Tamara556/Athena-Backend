package com.athena.rag.memory.chunking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class TokenEstimatorTest {

    private final TokenEstimator estimator = new TokenEstimator();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n\t"})
    void returnsZeroForBlank(String input) {
        assertThat(estimator.estimate(input)).isZero();
    }

    @ParameterizedTest
    @CsvSource({
            "a,1",
            "abcd,1",
            "abcde,2",
            "abcdefgh,2",
            "abcdefghi,3"
    })
    void estimatesCeilOfCharsOverFour(String text, int expectedTokens) {
        assertThat(estimator.estimate(text)).isEqualTo(expectedTokens);
    }

    @Test
    void scalesWithLength() {
        String small = "word";
        String large = small.repeat(50);
        assertThat(estimator.estimate(large)).isGreaterThan(estimator.estimate(small));
    }
}
