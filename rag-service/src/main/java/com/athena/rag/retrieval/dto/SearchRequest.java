package com.athena.rag.retrieval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;

import java.util.List;

public record SearchRequest(
        @NotBlank(message = "query must not be blank")
        String query,

        List<String> sourceTypes,

        String domain,

        @Min(value = 0, message = "page must not be negative")
        Integer page,

        @Min(value = 1, message = "size must be at least 1")
        Integer size
) {
}
