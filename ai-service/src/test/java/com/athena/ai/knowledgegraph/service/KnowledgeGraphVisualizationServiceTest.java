package com.athena.ai.knowledgegraph.service;

import com.athena.ai.knowledgegraph.domain.RelationshipType;
import com.athena.ai.knowledgegraph.dto.KnowledgeGraphVisualizationResponse;
import com.athena.ai.knowledgegraph.service.KnowledgeGraphVisualizationService;
import com.athena.ai.knowledgegraph.service.impl.KnowledgeGraphVisualizationServiceImpl;
import com.athena.ai.knowledgegraph.entity.KnowledgeEdge;
import com.athena.ai.knowledgegraph.entity.KnowledgeGraphSnapshot;
import com.athena.ai.knowledgegraph.entity.KnowledgeNode;
import com.athena.ai.generation.messaging.AiEventPublisher;
import com.athena.ai.knowledgegraph.repository.KnowledgeEdgeRepository;
import com.athena.ai.knowledgegraph.repository.KnowledgeGraphSnapshotRepository;
import com.athena.ai.knowledgegraph.repository.KnowledgeNodeRepository;
import com.athena.common.event.KafkaTopics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeGraphVisualizationServiceTest {

    @Mock private KnowledgeNodeRepository nodeRepository;
    @Mock private KnowledgeEdgeRepository edgeRepository;
    @Mock private KnowledgeGraphSnapshotRepository snapshotRepository;
    @Mock private AiEventPublisher events;

    private KnowledgeGraphVisualizationService service;

    private final UUID userId = UUID.fromString("88888888-8888-8888-8888-888888888888");

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-13T12:00:00Z"), ZoneOffset.UTC);
        service = new KnowledgeGraphVisualizationServiceImpl(nodeRepository, edgeRepository, snapshotRepository,
                events, JsonMapper.builder().build(), new SimpleMeterRegistry(), clock);
    }

    private KnowledgeNode node(String skill, String domain, int mastery, double confidence, String category) {
        KnowledgeNode n = new KnowledgeNode(userId, skill, domain, mastery, confidence);
        n.setCategory(category);
        return n;
    }

    @Test
    void getVisualization_buildsNodesEdgesSummaryAndInsights() {
        when(nodeRepository.findByUserIdOrderBySkillNameAsc(userId)).thenReturn(List.of(
                node("OOP", "Java", 90, 0.95, "CORE"),
                node("Collections", "Java", 65, 0.80, "CORE"),
                node("Concurrency", "Java", 20, 0.40, "ADVANCED")));
        when(edgeRepository.findByUserId(userId)).thenReturn(List.of(
                new KnowledgeEdge(userId, "OOP", "Collections", RelationshipType.PREREQUISITE),
                new KnowledgeEdge(userId, "Collections", "Concurrency", RelationshipType.SUPPORTS)));

        KnowledgeGraphVisualizationResponse viz = service.getVisualization(userId);

        assertThat(viz.domain()).isEqualTo("Java");
        assertThat(viz.nodes()).hasSize(3);
        assertThat(viz.nodes()).anySatisfy(n -> {
            if (n.id().equals("concurrency")) {
                assertThat(n.status()).isEqualTo("WEAKNESS");
                assertThat(n.confidence()).isEqualTo(40);
                assertThat(n.category()).isEqualTo("ADVANCED");
            }
        });
        assertThat(viz.edges()).hasSize(2);
        assertThat(viz.summary().averageMastery()).isEqualTo(58);
        assertThat(viz.summary().totalSkills()).isEqualTo(3);
        assertThat(viz.summary().strongestSkills()).containsExactly("OOP");
        assertThat(viz.summary().weakestSkills()).containsExactly("Concurrency");
        assertThat(viz.insights()).anyMatch(s -> s.contains("Improving Collections"));
    }

    @Test
    void onGraphChanged_publishesVisualizationEvent_andSkipsSnapshotWhenUnchanged() {
        when(nodeRepository.findByUserIdOrderBySkillNameAsc(userId))
                .thenReturn(List.of(node("OOP", "Java", 50, 0.5, "CORE")));
        when(edgeRepository.findByUserId(userId)).thenReturn(List.of());
        KnowledgeGraphSnapshot previous = new KnowledgeGraphSnapshot(userId, 50, "{}");
        when(snapshotRepository.findFirstByUserIdOrderByGeneratedAtDesc(userId)).thenReturn(Optional.of(previous));

        service.onGraphChanged(userId);

        verify(snapshotRepository, never()).save(any());
        verify(events).publish(eq(KafkaTopics.KNOWLEDGE_GRAPH_VISUALIZATION_GENERATED), eq(userId), any());
    }
}
