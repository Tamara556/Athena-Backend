package com.athena.rag.memory.dto;

import com.athena.rag.memory.domain.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IngestDocumentRequest(
        @NotBlank(message = "title must not be blank")
        @Size(max = 300, message = "title must be at most 300 characters")
        String title,

        @NotBlank(message = "content must not be blank")
        String content,

        @Size(max = 120, message = "learningDomain must be at most 120 characters")
        String learningDomain,

        @Size(max = 60, message = "category must be at most 60 characters")
        String category,

        Visibility visibility
) {
}
