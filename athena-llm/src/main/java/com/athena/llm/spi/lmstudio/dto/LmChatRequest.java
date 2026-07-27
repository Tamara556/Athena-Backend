package com.athena.llm.spi.lmstudio.dto;

import java.util.List;

public record LmChatRequest(
        String model,
        List<LmChatMessage> messages,
        double temperature,
        int max_tokens,
        boolean stream,
        LmResponseFormat response_format
) {

    public record LmChatMessage(String role, String content) {
    }
}
