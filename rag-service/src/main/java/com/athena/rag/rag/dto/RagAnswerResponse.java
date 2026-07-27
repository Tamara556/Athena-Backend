package com.athena.rag.rag.dto;

import java.util.List;

public record RagAnswerResponse(
        String answer,
        boolean grounded,
        int usedContextCount,
        Double topScore,
        List<Citation> citations
) {
}
