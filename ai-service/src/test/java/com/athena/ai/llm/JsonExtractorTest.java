package com.athena.ai.llm;

import com.athena.ai.client.AiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonExtractorTest {

    @Test
    void extractsPlainJsonObject() {
        assertThat(JsonExtractor.extract("{\"a\":1}")).isEqualTo("{\"a\":1}");
    }

    @Test
    void stripsCodeFences() {
        String raw = "```json\n{\"a\":1}\n```";
        assertThat(JsonExtractor.extract(raw)).isEqualTo("{\"a\":1}");
    }

    @Test
    void extractsJsonEmbeddedInProse() {
        String raw = "Sure! Here is your plan:\n{\"items\":[]}\nHope that helps.";
        assertThat(JsonExtractor.extract(raw)).isEqualTo("{\"items\":[]}");
    }

    @Test
    void extractsJsonArray() {
        assertThat(JsonExtractor.extract("[1,2,3]")).isEqualTo("[1,2,3]");
    }

    @Test
    void throwsWhenNoJson() {
        assertThatThrownBy(() -> JsonExtractor.extract("no json here"))
                .isInstanceOf(AiException.class);
    }

    @Test
    void throwsOnBlank() {
        assertThatThrownBy(() -> JsonExtractor.extract("  "))
                .isInstanceOf(AiException.class);
    }

    @Test
    void stripsThinkBlockWithBraces() {
        String raw = "<think>The roadmap should look like {phases:[...]} roughly.</think>\n"
                + "{\"phases\":[{\"name\":\"Foundations\"}]}";
        assertThat(JsonExtractor.extract(raw)).isEqualTo("{\"phases\":[{\"name\":\"Foundations\"}]}");
    }

    @Test
    void handlesClosingThinkTagWithoutOpening() {
        String raw = "I will draft {a stray brace} first.</think>\n{\"a\":1}";
        assertThat(JsonExtractor.extract(raw)).isEqualTo("{\"a\":1}");
    }

    @Test
    void extractsBalancedObjectIgnoringTrailingProse() {
        String raw = "{\"a\":{\"b\":2}} and that's the plan, enjoy!";
        assertThat(JsonExtractor.extract(raw)).isEqualTo("{\"a\":{\"b\":2}}");
    }

    @Test
    void ignoresBracesInsideStrings() {
        String raw = "{\"note\":\"use a } here\",\"ok\":true}";
        assertThat(JsonExtractor.extract(raw)).isEqualTo(raw);
    }

    @Test
    void repairsMissingArrayCloser() {
        String raw = "{\"phases\":[{\"name\":\"P1\",\"objectives\":[\"a\",\"b\"]},"
                + "{\"name\":\"P2\",\"objectives\":[\"c\",\"d\"}]}";
        String expected = "{\"phases\":[{\"name\":\"P1\",\"objectives\":[\"a\",\"b\"]},"
                + "{\"name\":\"P2\",\"objectives\":[\"c\",\"d\"]}]}";
        assertThat(JsonExtractor.extract(raw)).isEqualTo(expected);
    }

    @Test
    void closesTruncatedStructures() {
        assertThat(JsonExtractor.extract("{\"phases\":[{\"name\":\"Foun")).isEqualTo(
                "{\"phases\":[{\"name\":\"Foun\"}]}");
    }
}
