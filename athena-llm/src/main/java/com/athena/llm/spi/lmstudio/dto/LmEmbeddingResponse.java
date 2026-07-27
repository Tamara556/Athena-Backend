package com.athena.llm.spi.lmstudio.dto;

import java.util.List;

public record LmEmbeddingResponse(List<Data> data, Usage usage) {

    public record Data(int index, float[] embedding) {
    }

    public record Usage(int prompt_tokens, int total_tokens) {
    }

    public int totalTokens() {
        return usage == null ? 0 : usage.total_tokens();
    }
}
