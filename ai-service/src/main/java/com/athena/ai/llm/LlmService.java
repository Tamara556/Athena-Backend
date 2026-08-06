package com.athena.ai.llm;

import com.athena.llm.model.ResponseFormat;

import java.util.UUID;

public interface LlmService {

    <T> T generateJson(UUID userId, String promptType, String systemPrompt, String userPrompt, Class<T> type);

    <T> T generateJson(UUID userId, String promptType, String systemPrompt, String userPrompt,
                         Class<T> type, ResponseFormat responseFormat);
}
