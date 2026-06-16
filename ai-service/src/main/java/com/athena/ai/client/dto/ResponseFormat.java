package com.athena.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponseFormat(String type, @JsonProperty("json_schema") JsonSchema jsonSchema) {

    public static final ResponseFormat JSON = ofSchema("athena_response", Map.of("type", "object"), false);

    public static ResponseFormat ofSchema(String name, Map<String, Object> schema) {
        return ofSchema(name, schema, true);
    }

    private static ResponseFormat ofSchema(String name, Map<String, Object> schema, boolean strict) {
        return new ResponseFormat("json_schema", new JsonSchema(name, strict, schema));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record JsonSchema(String name, boolean strict, Map<String, Object> schema) {
    }
}
