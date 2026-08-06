package com.athena.llm.parser;

import com.athena.llm.LlmException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Pattern;

public final class StructuredOutputParser {

    private static final Pattern THINK_BLOCK = Pattern.compile("(?is)<think\\b[^>]*>.*?</think>");
    private static final Pattern THINKING_BLOCK = Pattern.compile("(?is)<thinking\\b[^>]*>.*?</thinking>");

    private StructuredOutputParser() {
    }

    public static String extract(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new LlmException("LLM returned an empty response where JSON was expected");
        }
        String text = stripThinkBlocks(raw);
        text = stripCodeFences(text.trim());

        String json = extractBalanced(text);
        if (json == null) {
            throw new LlmException("LLM response did not contain parseable JSON");
        }
        return json;
    }

    private static String stripThinkBlocks(String text) {
        String cleaned = THINK_BLOCK.matcher(text).replaceAll("");
        cleaned = THINKING_BLOCK.matcher(cleaned).replaceAll("");

        int lastClose = Math.max(
                cleaned.toLowerCase().lastIndexOf("</think>"),
                cleaned.toLowerCase().lastIndexOf("</thinking>"));
        if (lastClose >= 0) {
            cleaned = cleaned.substring(lastClose);
        }
        return cleaned;
    }

    private static String stripCodeFences(String text) {
        if (!text.contains("```")) {
            return text;
        }
        String withoutOpening = text.replaceFirst("(?s)```[a-zA-Z]*\\s*", "");
        int closing = withoutOpening.lastIndexOf("```");
        return closing >= 0 ? withoutOpening.substring(0, closing) : withoutOpening;
    }

    private static String extractBalanced(String text) {
        int start = firstJsonStart(text);
        if (start < 0) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        Deque<Character> stack = new ArrayDeque<>();
        boolean inString = false;
        boolean escaped = false;

        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                out.append(c);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                out.append(c);
            } else if (c == '{') {
                stack.push('}');
                out.append(c);
            } else if (c == '[') {
                stack.push(']');
                out.append(c);
            } else if (c == '}' || c == ']') {
                if (stack.isEmpty()) {
                    break;
                }
                while (!stack.isEmpty() && stack.peek() != c) {
                    out.append(stack.pop());
                }
                if (!stack.isEmpty()) {
                    out.append(stack.pop());
                }
                if (stack.isEmpty()) {
                    return out.toString();
                }
            } else {
                out.append(c);
            }
        }

        if (out.length() == 0) {
            return null;
        }
        if (inString) {
            out.append('"');
        }
        while (!stack.isEmpty()) {
            out.append(stack.pop());
        }
        return out.toString();
    }

    private static int firstJsonStart(String text) {
        int obj = text.indexOf('{');
        int arr = text.indexOf('[');
        if (obj < 0) return arr;
        if (arr < 0) return obj;
        return Math.min(obj, arr);
    }
}
