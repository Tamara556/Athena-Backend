package com.athena.ai.client;

public record AiCompletion(String content, int totalTokens, long latencyMs) {
}
