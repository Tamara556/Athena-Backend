package com.athena.llm.model;

import java.util.Map;

public record ResponseFormat(Kind kind, String schemaName, Map<String, Object> schema) {

    public enum Kind {
        TEXT,
        JSON,
        JSON_SCHEMA
    }

    public static final ResponseFormat TEXT = new ResponseFormat(Kind.TEXT, null, null);
    public static final ResponseFormat JSON = new ResponseFormat(Kind.JSON, null, null);

    public static ResponseFormat ofSchema(String name, Map<String, Object> schema) {
        return new ResponseFormat(Kind.JSON_SCHEMA, name, schema);
    }
}
