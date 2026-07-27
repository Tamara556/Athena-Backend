package com.athena.rag.memory.controller;

import com.athena.common.security.AuthHeaders;
import com.athena.rag.memory.domain.SourceType;
import com.athena.rag.memory.domain.Visibility;
import com.athena.rag.memory.dto.DocumentResponse;
import com.athena.rag.memory.dto.IngestDocumentRequest;
import com.athena.rag.memory.dto.ReindexResponse;
import com.athena.rag.memory.service.EmbeddingService;
import com.athena.rag.memory.service.IngestOutcome;
import com.athena.rag.memory.service.MemoryIngestCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final EmbeddingService embeddingService;

    @PostMapping("/rag/documents")
    public ResponseEntity<DocumentResponse> ingest(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                   @Valid @RequestBody IngestDocumentRequest request) {
        MemoryIngestCommand command = new MemoryIngestCommand(
                userId, SourceType.MATERIAL, null, request.learningDomain(),
                request.category() == null ? "MATERIAL" : request.category(),
                request.visibility() == null ? Visibility.PRIVATE : request.visibility(),
                request.title(), request.content());
        IngestOutcome outcome = embeddingService.ingest(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentResponse.from(outcome));
    }

    @DeleteMapping("/rag/documents/{documentId}")
    public ResponseEntity<Void> delete(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                       @PathVariable UUID documentId) {
        embeddingService.delete(userId, documentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/rag/reindex/me")
    public ResponseEntity<ReindexResponse> reindex(@RequestHeader(AuthHeaders.USER_ID) UUID userId) {
        return ResponseEntity.ok(ReindexResponse.from(embeddingService.reindexUser(userId)));
    }
}
