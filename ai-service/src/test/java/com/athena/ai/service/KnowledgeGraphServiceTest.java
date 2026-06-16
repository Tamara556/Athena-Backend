package com.athena.ai.service;

import com.athena.ai.dto.KnowledgeNodeResponse;
import com.athena.ai.entity.KnowledgeNode;
import com.athena.ai.messaging.AiEventPublisher;
import com.athena.ai.service.impl.KnowledgeGraphServiceImpl;
import com.athena.ai.repository.KnowledgeEdgeRepository;
import com.athena.ai.repository.KnowledgeNodeRepository;
import com.athena.common.event.KafkaTopics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeGraphServiceTest {

    @Mock private KnowledgeNodeRepository repository;
    @Mock private KnowledgeEdgeRepository edgeRepository;
    @Mock private AiEventPublisher events;
    @Mock private KnowledgeGraphVisualizationService visualization;

    private KnowledgeGraphService service;

    private final UUID userId = UUID.fromString("77777777-7777-7777-7777-777777777777");

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-13T10:00:00Z"), ZoneOffset.UTC);
        service = new KnowledgeGraphServiceImpl(repository, edgeRepository, events, visualization, clock);
    }

    @Test
    void recordWeaknesses_upsertsNodeAndPublishesEvent() {
        when(repository.findByUserIdOrderBySkillNameAsc(userId)).thenReturn(List.of());
        when(repository.findByUserIdAndSkillNameIgnoreCase(userId, "Concurrency")).thenReturn(Optional.empty());
        when(repository.save(any(KnowledgeNode.class))).thenAnswer(i -> i.getArgument(0));

        service.recordWeaknesses(userId, "Java", List.of("Concurrency"));

        verify(repository).save(any(KnowledgeNode.class));
        verify(events).publish(eq(KafkaTopics.KNOWLEDGE_GRAPH_UPDATED), eq(userId), any());
        verify(visualization).onGraphChanged(userId);
    }

    @Test
    void recordWeaknesses_noopOnEmpty() {
        service.recordWeaknesses(userId, "Java", List.of());
        verify(repository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void getForUser_mapsNodes() {
        KnowledgeNode node = new KnowledgeNode(userId, "Collections", "Java", 65, 0.6);
        when(repository.findByUserIdOrderBySkillNameAsc(userId)).thenReturn(List.of(node));

        List<KnowledgeNodeResponse> result = service.getForUser(userId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().skillName()).isEqualTo("Collections");
        assertThat(result.getFirst().masteryPercentage()).isEqualTo(65);
    }
}
