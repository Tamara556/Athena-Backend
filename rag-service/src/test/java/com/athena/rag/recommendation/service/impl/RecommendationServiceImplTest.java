package com.athena.rag.recommendation.service.impl;

import com.athena.llm.ChatProvider;
import com.athena.llm.model.ChatRequest;
import com.athena.llm.model.ChatResult;
import com.athena.rag.client.KnowledgeGraphClient;
import com.athena.rag.client.ProgressClient;
import com.athena.rag.config.RagProperties;
import com.athena.rag.observability.RagMetrics;
import com.athena.rag.rag.service.AssembledContext;
import com.athena.rag.rag.service.ContextAssembler;
import com.athena.rag.recommendation.dto.NextRecommendationRequest;
import com.athena.rag.recommendation.dto.NextRecommendationResponse;
import com.athena.rag.retrieval.domain.RetrievedChunk;
import com.athena.rag.retrieval.service.RetrievalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

    @Mock
    private RetrievalService retrievalService;
    @Mock
    private ContextAssembler contextAssembler;
    @Mock
    private ChatProvider chatProvider;
    @Mock
    private KnowledgeGraphClient knowledgeGraphClient;
    @Mock
    private ProgressClient progressClient;
    @Mock
    private RagMetrics metrics;

    private final RagProperties properties = new RagProperties(1024, 900, 150, 6, 0.35, 6000, 20);
    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private RecommendationServiceImpl service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new RecommendationServiceImpl(retrievalService, contextAssembler, chatProvider,
                knowledgeGraphClient, progressClient, properties, metrics, objectMapper);
    }

    @Test
    void returnsUngroundedWhenNoSignalsExist() {
        when(retrievalService.retrieve(any())).thenReturn(List.of());
        when(knowledgeGraphClient.getGraph(userId)).thenThrow(new RuntimeException("unavailable"));
        when(progressClient.getProgress(userId)).thenThrow(new RuntimeException("unavailable"));

        NextRecommendationResponse response = service.recommendNext(userId, new NextRecommendationRequest(null));

        assertThat(response.grounded()).isFalse();
        assertThat(response.recommendation()).contains("don't have enough");
        assertThat(response.focusAreas()).isEmpty();
        verify(metrics).ungrounded();
        verify(chatProvider, never()).complete(any());
    }

    @Test
    void producesGroundedRecommendationFromModelJson() {
        when(retrievalService.retrieve(any())).thenReturn(List.of(chunk()));
        when(knowledgeGraphClient.getGraph(userId)).thenThrow(new RuntimeException("skip"));
        when(progressClient.getProgress(userId)).thenThrow(new RuntimeException("skip"));
        when(contextAssembler.assemble(any()))
                .thenReturn(new AssembledContext("[1] LESSON\nbody", List.of(), 1, 0.8));
        String json = "{\"recommendation\":\"Study SQL joins\",\"rationale\":\"You struggled there\","
                + "\"focusAreas\":[\"joins\",\"indexes\"]}";
        when(chatProvider.complete(any(ChatRequest.class))).thenReturn(new ChatResult(json, 5, 6, 11, 20));

        NextRecommendationResponse response = service.recommendNext(userId, new NextRecommendationRequest("databases"));

        assertThat(response.grounded()).isTrue();
        assertThat(response.recommendation()).isEqualTo("Study SQL joins");
        assertThat(response.rationale()).isEqualTo("You struggled there");
        assertThat(response.focusAreas()).containsExactly("joins", "indexes");
    }

    @Test
    void recordsFailureAndRethrowsWhenModelFails() {
        when(retrievalService.retrieve(any())).thenReturn(List.of(chunk()));
        when(knowledgeGraphClient.getGraph(userId)).thenThrow(new RuntimeException("skip"));
        when(progressClient.getProgress(userId)).thenThrow(new RuntimeException("skip"));
        when(contextAssembler.assemble(any()))
                .thenReturn(new AssembledContext("ctx", List.of(), 1, 0.8));
        when(chatProvider.complete(any(ChatRequest.class))).thenThrow(new RuntimeException("model down"));

        assertThatThrownBy(() -> service.recommendNext(userId, new NextRecommendationRequest("databases")))
                .isInstanceOf(RuntimeException.class);

        verify(metrics).llmFailure();
    }

    private RetrievedChunk chunk() {
        return new RetrievedChunk(UUID.randomUUID(), UUID.randomUUID(), "LESSON", UUID.randomUUID(),
                "databases", "core", "Joins", "body", 0.8);
    }
}
