package com.athena.rag.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RagQueryRequest(
        @NotBlank(message = "query must not be blank")
        @Size(max = 2000, message = "query must be at most 2000 characters")
        String query,

        String domain,

        List<String> sourceTypes,

        Integer topK
) {
}
