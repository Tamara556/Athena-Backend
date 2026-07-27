package com.athena.rag.rag.service;

import com.athena.rag.rag.dto.Citation;

import java.util.List;

public record AssembledContext(
        String contextText,
        List<Citation> citations,
        int usedCount,
        double topScore
) {

    public boolean isEmpty() {
        return usedCount == 0;
    }
}
