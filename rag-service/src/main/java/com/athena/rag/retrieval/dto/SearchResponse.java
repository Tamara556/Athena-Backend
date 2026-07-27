package com.athena.rag.retrieval.dto;

import java.util.List;

public record SearchResponse(
        String query,
        int page,
        int size,
        int count,
        List<SearchResultItem> results
) {
}
