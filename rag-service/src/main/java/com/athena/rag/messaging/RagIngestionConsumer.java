package com.athena.rag.messaging;

import com.athena.common.event.BadgeAwardedEvent;
import com.athena.common.event.InterviewEvaluatedEvent;
import com.athena.common.event.KafkaTopics;
import com.athena.common.event.KnowledgeGraphUpdatedEvent;
import com.athena.common.event.LearningSessionCompletedEvent;
import com.athena.common.event.RoadmapGeneratedEvent;
import com.athena.rag.client.KnowledgeGraphClient;
import com.athena.rag.client.LearningSessionClient;
import com.athena.rag.client.RoadmapClient;
import com.athena.rag.client.dto.RoadmapView;
import com.athena.rag.memory.domain.SourceType;
import com.athena.rag.memory.domain.Visibility;
import com.athena.rag.memory.ingestion.JsonText;
import com.athena.rag.memory.ingestion.MemoryTextFactory;
import com.athena.rag.memory.service.EmbeddingService;
import com.athena.rag.memory.service.MemoryIngestCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RagIngestionConsumer {

    private final EmbeddingService embeddingService;
    private final MemoryTextFactory textFactory;
    private final RoadmapClient roadmapClient;
    private final LearningSessionClient learningSessionClient;
    private final KnowledgeGraphClient knowledgeGraphClient;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.INTERVIEW_EVALUATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onInterviewEvaluated(String payload) {
        InterviewEvaluatedEvent event = read(payload, InterviewEvaluatedEvent.class);
        ingest(new MemoryIngestCommand(event.userId(), SourceType.INTERVIEW, event.interviewId(),
                event.domain(), "INTERVIEW", Visibility.PRIVATE,
                "Interview: " + event.domain(), textFactory.interview(event)));
    }

    @KafkaListener(topics = KafkaTopics.ROADMAP_GENERATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onRoadmapGenerated(String payload) {
        RoadmapGeneratedEvent event = read(payload, RoadmapGeneratedEvent.class);
        try {
            RoadmapView roadmap = roadmapClient.getRoadmap(event.roadmapId());
            ingest(new MemoryIngestCommand(event.userId(), SourceType.ROADMAP, event.roadmapId(),
                    null, "ROADMAP", Visibility.PRIVATE,
                    "Roadmap: " + safe(roadmap.goal()), textFactory.roadmap(roadmap)));
        } catch (RuntimeException ex) {
            log.error("Failed to ingest roadmap userId={} roadmapId={} cause={}",
                    event.userId(), event.roadmapId(), ex.getClass().getSimpleName());
        }
    }

    @KafkaListener(topics = KafkaTopics.LEARNING_SESSION_COMPLETED, groupId = "${spring.kafka.consumer.group-id}")
    public void onLearningSessionCompleted(String payload) {
        LearningSessionCompletedEvent event = read(payload, LearningSessionCompletedEvent.class);
        try {
            JsonNode session = learningSessionClient.getSession(event.userId(), event.sessionId());
            String title = JsonText.firstText(session, "title", "nodeTitle", "node", "goal");
            ingest(new MemoryIngestCommand(event.userId(), SourceType.LEARNING_SESSION, event.sessionId(),
                    JsonText.firstText(session, "domain"), "LEARNING_SESSION", Visibility.PRIVATE,
                    title == null ? "Learning session" : title, JsonText.flatten(session)));
        } catch (RuntimeException ex) {
            log.error("Failed to ingest learning session userId={} sessionId={} cause={}",
                    event.userId(), event.sessionId(), ex.getClass().getSimpleName());
        }
    }

    @KafkaListener(topics = KafkaTopics.KNOWLEDGE_GRAPH_UPDATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onKnowledgeGraphUpdated(String payload) {
        KnowledgeGraphUpdatedEvent event = read(payload, KnowledgeGraphUpdatedEvent.class);
        try {
            JsonNode graph = knowledgeGraphClient.getGraph(event.userId());
            ingest(new MemoryIngestCommand(event.userId(), SourceType.KNOWLEDGE_GRAPH, event.userId(),
                    null, "KNOWLEDGE_GRAPH", Visibility.PRIVATE,
                    "Knowledge graph", JsonText.flatten(graph)));
        } catch (RuntimeException ex) {
            log.error("Failed to ingest knowledge graph userId={} cause={}",
                    event.userId(), ex.getClass().getSimpleName());
        }
    }

    @KafkaListener(topics = KafkaTopics.BADGE_AWARDED, groupId = "${spring.kafka.consumer.group-id}")
    public void onBadgeAwarded(String payload) {
        BadgeAwardedEvent event = read(payload, BadgeAwardedEvent.class);
        UUID entityId = UUID.nameUUIDFromBytes(
                (event.userId() + ":" + event.badgeCode()).getBytes(StandardCharsets.UTF_8));
        ingest(new MemoryIngestCommand(event.userId(), SourceType.ACHIEVEMENT, entityId,
                null, "ACHIEVEMENT", Visibility.PRIVATE,
                "Achievement: " + event.badgeCode(), textFactory.achievement(event.badgeCode())));
    }

    private void ingest(MemoryIngestCommand command) {
        try {
            embeddingService.ingest(command);
        } catch (RuntimeException ex) {
            log.error("Ingestion failed userId={} sourceType={} entityId={} cause={}",
                    command.userId(), command.sourceType(), command.entityId(), ex.getClass().getSimpleName());
        }
    }

    private <T> T read(String payload, Class<T> type) {
        return objectMapper.readValue(payload, type);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
