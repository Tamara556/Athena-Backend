package com.athena.rag.memory.chunking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContentPreprocessorTest {

    private static final char BELL = (char) 0x07;
    private static final char NUL = (char) 0x00;

    private final ContentPreprocessor preprocessor = new ContentPreprocessor();

    @Test
    void returnsEmptyForNull() {
        assertThat(preprocessor.clean(null)).isEmpty();
    }

    @Test
    void collapsesHorizontalWhitespace() {
        assertThat(preprocessor.clean("a   b\t\tc")).isEqualTo("a b c");
    }

    @Test
    void collapsesThreeOrMoreNewlinesToDouble() {
        assertThat(preprocessor.clean("a\n\n\n\n\nb")).isEqualTo("a\n\nb");
    }

    @Test
    void preservesSingleAndDoubleNewlines() {
        assertThat(preprocessor.clean("a\nb\n\nc")).isEqualTo("a\nb\n\nc");
    }

    @Test
    void stripsControlCharactersToSpaces() {
        String withControls = "x" + NUL + "y" + BELL + "z";
        assertThat(preprocessor.clean(withControls)).isEqualTo("x y z");
    }

    @Test
    void keepsTabAndNewlineControlCharacters() {
        assertThat(preprocessor.clean("line1\nline2")).isEqualTo("line1\nline2");
    }

    @Test
    void trimsLeadingAndTrailingWhitespace() {
        assertThat(preprocessor.clean("   hello   ")).isEqualTo("hello");
    }

    @Test
    void truncatesToMaxLength() {
        String huge = "x".repeat(250_000);
        assertThat(preprocessor.clean(huge)).hasSize(200_000);
    }
}
