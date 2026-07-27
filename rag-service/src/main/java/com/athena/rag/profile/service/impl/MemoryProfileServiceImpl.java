package com.athena.rag.profile.service.impl;

import com.athena.rag.client.KnowledgeGraphClient;
import com.athena.rag.client.ProgressClient;
import com.athena.rag.memory.domain.SourceType;
import com.athena.rag.memory.entity.MemoryDocument;
import com.athena.rag.memory.ingestion.JsonText;
import com.athena.rag.memory.repository.MemoryDocumentRepository;
import com.athena.rag.profile.dto.MemoryProfileResponse;
import com.athena.rag.profile.service.MemoryProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryProfileServiceImpl implements MemoryProfileService {

    private static final int RECENT_LIMIT = 10;
    private static final int SUMMARY_MAX_CHARS = 1500;

    private final MemoryDocumentRepository documentRepository;
    private final KnowledgeGraphClient knowledgeGraphClient;
    private final ProgressClient progressClient;

    @Override
    @Transactional(readOnly = true)
    public MemoryProfileResponse getProfile(UUID userId) {
        List<MemoryDocument> documents = documentRepository.findByUserIdOrderByUpdatedAtDesc(userId);

        Map<SourceType, Long> counts = documents.stream()
                .collect(Collectors.groupingBy(MemoryDocument::getSourceType, Collectors.counting()));
        List<MemoryProfileResponse.SourceCount> sources = counts.entrySet().stream()
                .map(e -> new MemoryProfileResponse.SourceCount(e.getKey(), e.getValue()))
                .sorted((a, b) -> Long.compare(b.count(), a.count()))
                .toList();

        List<MemoryProfileResponse.MemoryItem> recent = documents.stream()
                .limit(RECENT_LIMIT)
                .map(this::toItem)
                .toList();

        return new MemoryProfileResponse(userId, documents.size(), sources, recent,
                summarize(() -> JsonText.flatten(knowledgeGraphClient.getGraph(userId))),
                summarize(() -> JsonText.flatten(progressClient.getProgress(userId))));
    }

    private MemoryProfileResponse.MemoryItem toItem(MemoryDocument document) {
        return new MemoryProfileResponse.MemoryItem(document.getSourceType(), document.getTitle(),
                document.getEntityId(), document.getLearningDomain(), document.getUpdatedAt());
    }

    private String summarize(Supplier<String> supplier) {
        try {
            String text = supplier.get();
            if (text == null || text.isBlank()) {
                return null;
            }
            return text.length() <= SUMMARY_MAX_CHARS ? text : text.substring(0, SUMMARY_MAX_CHARS).strip() + "…";
        } catch (RuntimeException ex) {
            log.warn("Profile enrichment unavailable cause={}", ex.getClass().getSimpleName());
            return null;
        }
    }
}
