package com.athena.rag.rag.controller;

import com.athena.common.security.AuthHeaders;
import com.athena.rag.rag.dto.RagAnswerResponse;
import com.athena.rag.rag.dto.RagQueryRequest;
import com.athena.rag.rag.service.RagQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RagController {

    private final RagQueryService ragQueryService;

    @PostMapping("/rag/query")
    public ResponseEntity<RagAnswerResponse> query(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                   @Valid @RequestBody RagQueryRequest request) {
        return ResponseEntity.ok(ragQueryService.answer(userId, request));
    }
}
