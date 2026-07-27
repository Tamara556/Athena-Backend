package com.athena.llm.spi.lmstudio.dto;

import java.util.List;

public record LmChatResponse(List<Choice> choices, Usage usage) {

    public record Choice(Message message) {
    }

    public record Message(String content) {
    }

    public record Usage(int prompt_tokens, int completion_tokens, int total_tokens) {
    }

    public boolean hasContent() {
        return choices != null && !choices.isEmpty()
                && choices.getFirst().message() != null
                && choices.getFirst().message().content() != null
                && !choices.getFirst().message().content().isBlank();
    }

    public String firstContent() {
        return choices.getFirst().message().content();
    }

    public int promptTokens() {
        return usage == null ? 0 : usage.prompt_tokens();
    }

    public int completionTokens() {
        return usage == null ? 0 : usage.completion_tokens();
    }

    public int totalTokens() {
        return usage == null ? 0 : usage.total_tokens();
    }
}
