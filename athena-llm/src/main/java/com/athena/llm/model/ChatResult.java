package com.athena.llm.model;

public record ChatResult(
        String content,
        int promptTokens,
        int completionTokens,
        int totalTokens,
        long latencyMs
) {

    public boolean hasContent() {
        return content != null && !content.isBlank();
    }
}
