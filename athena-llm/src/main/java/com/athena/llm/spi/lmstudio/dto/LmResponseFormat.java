package com.athena.llm.spi.lmstudio.dto;

import java.util.Map;

public record LmResponseFormat(String type, JsonSchema json_schema) {

    public record JsonSchema(String name, Map<String, Object> schema, boolean strict) {
    }

    public static LmResponseFormat text() {
        return new LmResponseFormat("text", null);
    }

    public static LmResponseFormat json() {
        return new LmResponseFormat("json_object", null);
    }

    public static LmResponseFormat schema(String name, Map<String, Object> schema) {
        return new LmResponseFormat("json_schema", new JsonSchema(name, schema, true));
    }
}
