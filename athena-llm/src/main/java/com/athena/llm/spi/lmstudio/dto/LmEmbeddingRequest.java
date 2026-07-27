package com.athena.llm.spi.lmstudio.dto;

import java.util.List;

public record LmEmbeddingRequest(String model, List<String> input) {
}
