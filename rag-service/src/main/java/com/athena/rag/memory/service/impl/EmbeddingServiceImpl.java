package com.athena.rag.memory.service.impl;

import com.athena.llm.EmbeddingProvider;
import com.athena.llm.model.EmbeddingResult;
import com.athena.rag.memory.chunking.Chunk;
import com.athena.rag.memory.chunking.ContentHashing;
import com.athena.rag.memory.chunking.ContentPreprocessor;
import com.athena.rag.memory.chunking.TextChunker;
import com.athena.rag.memory.domain.DocumentStatus;
import com.athena.rag.memory.entity.MemoryChunk;
import com.athena.rag.memory.entity.MemoryDocument;
import com.athena.rag.memory.repository.ChunkInsert;
import com.athena.rag.memory.repository.ChunkVectorRepository;
import com.athena.rag.memory.repository.MemoryChunkRepository;
import com.athena.rag.memory.repository.MemoryDocumentRepository;
import com.athena.rag.memory.service.EmbeddingService;
import com.athena.rag.memory.service.IngestOutcome;
import com.athena.rag.memory.service.MemoryIngestCommand;
import com.athena.rag.memory.service.ReindexOutcome;
import com.athena.rag.messaging.RagEventPublisher;
import com.athena.rag.observability.RagMetrics;
import com.athena.common.exception.ResourceNotFoundException;
import com.athena.rag.constants.RagConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    private final MemoryDocumentRepository documentRepository;
    private final MemoryChunkRepository chunkRepository;
    private final ChunkVectorRepository vectorRepository;
    private final ContentPreprocessor preprocessor;
    private final TextChunker chunker;
    private final EmbeddingProvider embeddingProvider;
    private final RagEventPublisher eventPublisher;
    private final RagMetrics metrics;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    @Transactional
    public IngestOutcome ingest(MemoryIngestCommand command) {
        String cleaned = preprocessor.clean(command.content());
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException("Cannot ingest empty content for source " + command.sourceType());
        }
        String hash = ContentHashing.sha256(cleaned);

        Optional<MemoryDocument> existing = command.entityId() == null ? Optional.empty()
                : documentRepository.findByUserIdAndSourceTypeAndEntityId(
                command.userId(), command.sourceType(), command.entityId());

        if (existing.isPresent() && hash.equals(existing.get().getContentHash())
                && existing.get().getStatus() == DocumentStatus.INDEXED) {
            MemoryDocument doc = existing.get();
            log.info("Skipping re-embed userId={} sourceType={} entityId={} reason=unchanged",
                    command.userId(), command.sourceType(), command.entityId());
            return new IngestOutcome(doc.getId(), doc.getSourceType(),
                    chunkRepository.findByDocumentIdOrderByChunkIndexAsc(doc.getId()).size(), doc.getStatus(), true);
        }

        MemoryDocument document = existing.map(doc -> {
            doc.setLearningDomain(command.learningDomain());
            doc.setCategory(command.category());
            doc.setVisibility(command.visibility());
            doc.setTitle(command.title());
            doc.setContentHash(hash);
            doc.setStatus(DocumentStatus.INDEXED);
            return documentRepository.save(doc);
        }).orElseGet(() -> documentRepository.save(new MemoryDocument(
                command.userId(), command.sourceType(), command.entityId(), command.learningDomain(),
                command.category(), command.visibility(), command.title(), hash)));

        List<Chunk> chunks = chunker.chunk(cleaned);
        int chunkCount = embedAndStore(document, chunks);

        eventPublisher.publishIndexed(document, chunkCount);
        return new IngestOutcome(document.getId(), document.getSourceType(), chunkCount, document.getStatus(), false);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID documentId) {
        MemoryDocument document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of(RagConstants.RESOURCE_DOCUMENT, documentId));
        vectorRepository.deleteByDocumentId(document.getId());
        documentRepository.delete(document);
        log.info("Deleted memory document userId={} documentId={}", userId, documentId);
    }

    @Override
    @Transactional
    public ReindexOutcome reindexUser(UUID userId) {
        List<MemoryDocument> documents = documentRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        int totalChunks = 0;
        int reindexed = 0;
        for (MemoryDocument document : documents) {
            List<MemoryChunk> stored = chunkRepository.findByDocumentIdOrderByChunkIndexAsc(document.getId());
            if (stored.isEmpty()) {
                continue;
            }
            List<Chunk> chunks = new ArrayList<>(stored.size());
            for (MemoryChunk chunk : stored) {
                chunks.add(new Chunk(chunk.getChunkIndex(), chunk.getContent(), chunk.getTokenCount()));
            }
            totalChunks += embedAndStore(document, chunks);
            reindexed++;
        }
        log.info("Reindexed userId={} documents={} chunks={}", userId, reindexed, totalChunks);
        return new ReindexOutcome(reindexed, totalChunks);
    }

    private int embedAndStore(MemoryDocument document, List<Chunk> chunks) {
        vectorRepository.deleteByDocumentId(document.getId());
        if (chunks.isEmpty()) {
            document.setStatus(DocumentStatus.INDEXED);
            documentRepository.save(document);
            return 0;
        }

        List<String> texts = chunks.stream().map(Chunk::content).toList();
        EmbeddingResult embeddings;
        try {
            embeddings = embeddingProvider.embed(texts);
            metrics.recordEmbeddingLatency(embeddings.latencyMs());
        } catch (RuntimeException ex) {
            metrics.embeddingFailure();
            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);
            throw ex;
        }

        Instant now = clock.instant();
        List<ChunkInsert> inserts = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            inserts.add(new ChunkInsert(UUID.randomUUID(), document.getId(), document.getUserId(), chunk.index(),
                    chunk.content(), chunk.tokenCount(), embeddings.vectors().get(i),
                    metadata(document, chunk, now), now));
        }

        vectorRepository.insertBatch(inserts);
        document.setStatus(DocumentStatus.INDEXED);
        documentRepository.save(document);
        return chunks.size();
    }

    private String metadata(MemoryDocument document, Chunk chunk, Instant createdAt) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("userId", document.getUserId().toString());
        metadata.put("sourceType", document.getSourceType().name());
        metadata.put("entityId", document.getEntityId() == null ? null : document.getEntityId().toString());
        metadata.put("category", document.getCategory());
        metadata.put("learningDomain", document.getLearningDomain());
        metadata.put("visibility", document.getVisibility().name());
        metadata.put("title", document.getTitle());
        metadata.put("chunkIndex", chunk.index());
        metadata.put("tokenCount", chunk.tokenCount());
        metadata.put("createdAt", createdAt.toString());
        return objectMapper.writeValueAsString(metadata);
    }
}
