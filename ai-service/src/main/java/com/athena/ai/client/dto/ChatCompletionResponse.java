package com.athena.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatCompletionResponse(
        List<Choice> choices,
        Usage usage
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(ChatMessage message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
            @JsonProperty("prompt_tokens") int promptTokens,
            @JsonProperty("completion_tokens") int completionTokens,
            @JsonProperty("total_tokens") int totalTokens
    ) {
    }

    public boolean hasContent() {
        return choices != null && !choices.isEmpty()
                && choices.getFirst().message() != null
                && choices.getFirst().message().content() != null;
    }

    public String firstContent() {
        return choices.getFirst().message().content();
    }

    public int totalTokens() {
        return usage == null ? 0 : usage.totalTokens();
    }
}
