package com.athena.llm.model;

import java.util.List;

public record ChatRequest(
        List<ChatMessage> messages,
        ResponseFormat responseFormat,
        Double temperature,
        Integer maxTokens
) {

    public static ChatRequest of(String systemPrompt, String userPrompt) {
        return new ChatRequest(
                List.of(ChatMessage.system(systemPrompt), ChatMessage.user(userPrompt)),
                ResponseFormat.TEXT, null, null);
    }

    public static ChatRequest of(String systemPrompt, String userPrompt, ResponseFormat responseFormat) {
        return new ChatRequest(
                List.of(ChatMessage.system(systemPrompt), ChatMessage.user(userPrompt)),
                responseFormat, null, null);
    }
}
