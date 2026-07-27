package com.athena.rag.memory.ingestion;

import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Set;

public final class JsonText {

    private static final Set<String> SKIP_KEYS = Set.of("status", "type", "practicetype", "videoid", "videoquery");
    private static final int MIN_LENGTH = 3;

    public static String flatten(JsonNode node) {
        StringBuilder sb = new StringBuilder();
        walk(node, sb);
        return sb.toString().strip();
    }

    public static String firstText(JsonNode node, String... fields) {
        if (node == null || !node.isObject()) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isTextual() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    private static void walk(JsonNode node, StringBuilder sb) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            for (Map.Entry<String, JsonNode> entry : node.properties()) {
                if (isSkippable(entry.getKey())) {
                    continue;
                }
                walk(entry.getValue(), sb);
            }
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                walk(element, sb);
            }
        } else if (node.isTextual()) {
            String value = node.asText().strip();
            if (value.length() >= MIN_LENGTH) {
                sb.append(value).append('\n');
            }
        }
    }

    private static boolean isSkippable(String key) {
        String lower = key.toLowerCase();
        return lower.endsWith("id") || SKIP_KEYS.contains(lower);
    }

    private JsonText() {
    }
}
