package com.athena.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ChatCompletionRequest(
        String model,
        List<ChatMessage> messages,
        double temperature,
        @JsonProperty("max_tokens") int maxTokens,
        boolean stream,
        @JsonProperty("response_format")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        ResponseFormat responseFormat
) {
}
