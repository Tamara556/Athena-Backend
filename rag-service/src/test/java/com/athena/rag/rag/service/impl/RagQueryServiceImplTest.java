package com.athena.rag.rag.service.impl;

import com.athena.llm.ChatProvider;
import com.athena.llm.model.ChatRequest;
import com.athena.llm.model.ChatResult;
import com.athena.rag.config.RagProperties;
import com.athena.rag.rag.dto.RagAnswerResponse;
import com.athena.rag.rag.dto.RagQueryRequest;
import com.athena.rag.rag.entity.RagQueryLog;
import com.athena.rag.rag.repository.RagQueryLogRepository;
import com.athena.rag.rag.service.AssembledContext;
import com.athena.rag.rag.service.ContextAssembler;
import com.athena.rag.rag.service.GroundingPolicy;
import com.athena.rag.rag.service.PromptBuilder;
import com.athena.rag.retrieval.domain.RetrievedChunk;
import com.athena.rag.retrieval.service.RetrievalRequest;
import com.athena.rag.retrieval.service.RetrievalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagQueryServiceImplTest {

    @Mock
    private RetrievalService retrievalService;
    @Mock
    private ContextAssembler contextAssembler;
    @Mock
    private GroundingPolicy groundingPolicy;
    @Mock
    private ChatProvider chatProvider;
    @Mock
    private RagQueryLogRepository queryLogRepository;
    @Mock
    private com.athena.rag.observability.RagMetrics metrics;

    private final RagProperties properties = new RagProperties(1024, 900, 150, 6, 0.35, 6000, 20);
    private RagQueryServiceImpl service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new RagQueryServiceImpl(retrievalService, contextAssembler, new PromptBuilder(),
                groundingPolicy, chatProvider, queryLogRepository, metrics, properties);
    }

    @Test
    void returnsUngroundedAnswerWithoutCallingTheModel() {
        when(retrievalService.retrieve(any())).thenReturn(List.of());
        when(groundingPolicy.isGrounded(anyList())).thenReturn(false);

        RagAnswerResponse response = service.answer(userId, new RagQueryRequest("who am I?", null, null, null));

        assertThat(response.grounded()).isFalse();
        assertThat(response.answer()).contains("don't have enough");
        assertThat(response.citations()).isEmpty();
        verify(chatProvider, never()).complete(any());
        verify(metrics).ungrounded();
        ArgumentCaptor<RagQueryLog> log = ArgumentCaptor.forClass(RagQueryLog.class);
        verify(queryLogRepository).save(log.capture());
        assertThat(log.getValue().isGrounded()).isFalse();
    }

    @Test
    void answersFromRetrievedContextWhenGrounded() {
        List<RetrievedChunk> chunks = List.of(chunk(0.9));
        when(retrievalService.retrieve(any())).thenReturn(chunks);
        when(groundingPolicy.isGrounded(chunks)).thenReturn(true);
        when(contextAssembler.assemble(chunks))
                .thenReturn(new AssembledContext("[1] LESSON\nbody", List.of(), 1, 0.9));
        when(chatProvider.complete(any(ChatRequest.class)))
                .thenReturn(new ChatResult("  Here is your grounded answer.  ", 10, 20, 30, 42));

        RagAnswerResponse response = service.answer(userId, new RagQueryRequest("explain", null, null, null));

        assertThat(response.grounded()).isTrue();
        assertThat(response.answer()).isEqualTo("Here is your grounded answer.");
        assertThat(response.usedContextCount()).isEqualTo(1);
        assertThat(response.topScore()).isEqualTo(0.9);
        verify(metrics).recordQueryLatency(org.mockito.ArgumentMatchers.anyLong());
        verify(queryLogRepository).save(any(RagQueryLog.class));
    }

    @Test
    void recordsFailureAndRethrowsWhenModelFails() {
        List<RetrievedChunk> chunks = List.of(chunk(0.9));
        when(retrievalService.retrieve(any())).thenReturn(chunks);
        when(groundingPolicy.isGrounded(chunks)).thenReturn(true);
        when(contextAssembler.assemble(chunks))
                .thenReturn(new AssembledContext("ctx", List.of(), 1, 0.9));
        when(chatProvider.complete(any(ChatRequest.class))).thenThrow(new RuntimeException("model down"));

        assertThatThrownBy(() -> service.answer(userId, new RagQueryRequest("explain", null, null, null)))
                .isInstanceOf(RuntimeException.class);

        verify(metrics).llmFailure();
        ArgumentCaptor<RagQueryLog> log = ArgumentCaptor.forClass(RagQueryLog.class);
        verify(queryLogRepository).save(log.capture());
        assertThat(log.getValue().getStatus()).isEqualTo("FAILED");
    }

    @Test
    void clampsRequestedTopKToMaxSearchResults() {
        when(retrievalService.retrieve(any())).thenReturn(List.of());
        when(groundingPolicy.isGrounded(anyList())).thenReturn(false);

        service.answer(userId, new RagQueryRequest("q", null, null, 999));

        ArgumentCaptor<RetrievalRequest> captor = ArgumentCaptor.forClass(RetrievalRequest.class);
        verify(retrievalService).retrieve(captor.capture());
        assertThat(captor.getValue().topK()).isEqualTo(properties.maxSearchResults());
    }

    private RetrievedChunk chunk(double score) {
        return new RetrievedChunk(UUID.randomUUID(), UUID.randomUUID(), "LESSON", UUID.randomUUID(),
                "math", "core", "Intro", "body", score);
    }
}
