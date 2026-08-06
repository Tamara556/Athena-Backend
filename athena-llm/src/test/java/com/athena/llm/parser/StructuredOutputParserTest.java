package com.athena.llm.parser;

import com.athena.llm.LlmException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StructuredOutputParserTest {

    @Test
    void extractsPlainJsonObject() {
        assertEquals("{\"a\":1}", StructuredOutputParser.extract("{\"a\":1}"));
    }

    @Test
    void stripsCodeFences() {
        String raw = "```json\n{\"a\":1}\n```";
        assertEquals("{\"a\":1}", StructuredOutputParser.extract(raw));
    }

    @Test
    void extractsJsonEmbeddedInProse() {
        String raw = "Sure! Here is your plan:\n{\"items\":[]}\nHope that helps.";
        assertEquals("{\"items\":[]}", StructuredOutputParser.extract(raw));
    }

    @Test
    void extractsJsonArray() {
        assertEquals("[1,2,3]", StructuredOutputParser.extract("[1,2,3]"));
    }

    @Test
    void throwsWhenNoJson() {
        assertThrows(LlmException.class, () -> StructuredOutputParser.extract("no json here"));
    }

    @Test
    void throwsOnBlank() {
        assertThrows(LlmException.class, () -> StructuredOutputParser.extract("  "));
    }

    @Test
    void stripsThinkBlockWithBraces() {
        String raw = "<think>The roadmap should look like {phases:[...]} roughly.</think>\n"
                + "{\"phases\":[{\"name\":\"Foundations\"}]}";
        assertEquals("{\"phases\":[{\"name\":\"Foundations\"}]}", StructuredOutputParser.extract(raw));
    }

    @Test
    void handlesClosingThinkTagWithoutOpening() {
        String raw = "I will draft {a stray brace} first.</think>\n{\"a\":1}";
        assertEquals("{\"a\":1}", StructuredOutputParser.extract(raw));
    }

    @Test
    void extractsBalancedObjectIgnoringTrailingProse() {
        String raw = "{\"a\":{\"b\":2}} and that's the plan, enjoy!";
        assertEquals("{\"a\":{\"b\":2}}", StructuredOutputParser.extract(raw));
    }

    @Test
    void ignoresBracesInsideStrings() {
        String raw = "{\"note\":\"use a } here\",\"ok\":true}";
        assertEquals(raw, StructuredOutputParser.extract(raw));
    }

    @Test
    void repairsMissingArrayCloser() {
        String raw = "{\"phases\":[{\"name\":\"P1\",\"objectives\":[\"a\",\"b\"]},"
                + "{\"name\":\"P2\",\"objectives\":[\"c\",\"d\"}]}";
        String expected = "{\"phases\":[{\"name\":\"P1\",\"objectives\":[\"a\",\"b\"]},"
                + "{\"name\":\"P2\",\"objectives\":[\"c\",\"d\"]}]}";
        assertEquals(expected, StructuredOutputParser.extract(raw));
    }

    @Test
    void closesTruncatedStructures() {
        assertEquals("{\"phases\":[{\"name\":\"Foun\"}]}",
                StructuredOutputParser.extract("{\"phases\":[{\"name\":\"Foun"));
    }
}
