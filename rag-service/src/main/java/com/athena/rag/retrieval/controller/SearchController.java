package com.athena.rag.retrieval.controller;

import com.athena.common.security.AuthHeaders;
import com.athena.rag.config.RagProperties;
import com.athena.rag.retrieval.domain.RetrievedChunk;
import com.athena.rag.retrieval.dto.SearchRequest;
import com.athena.rag.retrieval.dto.SearchResponse;
import com.athena.rag.retrieval.dto.SearchResultItem;
import com.athena.rag.retrieval.service.RetrievalRequest;
import com.athena.rag.retrieval.service.RetrievalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SearchController {

    private static final int SNIPPET_LENGTH = 280;

    private final RetrievalService retrievalService;
    private final RagProperties properties;

    @PostMapping("/rag/search")
    public ResponseEntity<SearchResponse> search(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                 @Valid @RequestBody SearchRequest request) {
        int page = request.page() == null ? 0 : request.page();
        int size = request.size() == null ? properties.retrievalTopK()
                : Math.min(request.size(), properties.maxSearchResults());

        RetrievalRequest retrievalRequest = new RetrievalRequest(
                userId, request.query(), request.sourceTypes(), request.domain(),
                size, properties.minSimilarity(), true, page * size);

        List<SearchResultItem> results = retrievalService.retrieve(retrievalRequest).stream()
                .map(chunk -> SearchResultItem.from(chunk, SNIPPET_LENGTH))
                .toList();

        return ResponseEntity.ok(new SearchResponse(request.query(), page, size, results.size(), results));
    }
}
