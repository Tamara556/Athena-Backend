package com.athena.rag.messaging;

import com.athena.common.event.KafkaTopics;
import com.athena.common.event.MemoryDocumentIndexedEvent;
import com.athena.rag.memory.entity.MemoryDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class RagEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishIndexed(MemoryDocument document, int chunkCount) {
        MemoryDocumentIndexedEvent event = new MemoryDocumentIndexedEvent(
                document.getUserId(), document.getId(), document.getSourceType().name(), chunkCount, Instant.now());
        String payload = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(KafkaTopics.MEMORY_DOCUMENT_INDEXED, document.getUserId().toString(), payload);
        log.info("Published MemoryDocumentIndexedEvent userId={} documentId={} sourceType={} chunks={}",
                document.getUserId(), document.getId(), document.getSourceType(), chunkCount);
    }
}
