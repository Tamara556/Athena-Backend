package com.athena.ai.llm;

import java.util.List;
import java.util.Map;

/**
 * Small builder for JSON-schema fragments used to constrain structured LLM output.
 * Produces plain {@code Map<String, Object>} nodes suitable for
 * {@link com.athena.llm.model.ResponseFormat#ofSchema(String, Map)}.
 */
public final class JsonSchema {

    public static final Map<String, Object> STR = Map.of("type", "string");
    public static final Map<String, Object> INT = Map.of("type", "integer");
    public static final Map<String, Object> NUM = Map.of("type", "number");

    private JsonSchema() {
    }

    public static Map<String, Object> object(Map<String, Object> properties, List<String> required) {
        return Map.of("type", "object", "properties", properties, "required", required);
    }

    public static Map<String, Object> arrayOf(Map<String, Object> items) {
        return Map.of("type", "array", "items", items);
    }

    public static Map<String, Object> enumOf(List<String> values) {
        return Map.of("type", "string", "enum", values);
    }
}
