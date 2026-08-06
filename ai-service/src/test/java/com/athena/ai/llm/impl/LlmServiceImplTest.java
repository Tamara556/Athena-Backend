package com.athena.ai.llm.impl;

import com.athena.ai.constants.AiConstants;
import com.athena.ai.generation.entity.AiRequest;
import com.athena.ai.generation.entity.AiResponse;
import com.athena.ai.generation.observability.AiMetrics;
import com.athena.ai.generation.repository.AiRequestRepository;
import com.athena.ai.generation.repository.AiResponseRepository;
import com.athena.ai.llm.AiException;
import com.athena.llm.ChatProvider;
import com.athena.llm.LlmException;
import com.athena.llm.model.ChatRequest;
import com.athena.llm.model.ChatResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlmServiceImplTest {

    @Mock
    private ChatProvider chatProvider;
    @Mock
    private AiRequestRepository requestRepository;
    @Mock
    private AiResponseRepository responseRepository;
    @Mock
    private AiMetrics metrics;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private LlmServiceImpl service;

    private final UUID userId = UUID.randomUUID();

    public record Reply(String reply) {
    }

    @BeforeEach
    void setUp() {
        service = new LlmServiceImpl(chatProvider, requestRepository, responseRepository, metrics, objectMapper);
        when(requestRepository.save(any(AiRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chatProvider.model()).thenReturn("test-model");
    }

    @Test
    void persistsSuccessTelemetryAndRecordsMetricsOnHappyPath() {
        when(chatProvider.complete(any(ChatRequest.class)))
                .thenReturn(new ChatResult("{\"reply\":\"hello\"}", 12, 8, 20, 55));

        Reply reply = service.generateJson(userId, "MENTOR_REPLY", "sys", "user", Reply.class);

        assertThat(reply.reply()).isEqualTo("hello");
        // PENDING then SUCCESS -> two request saves.
        ArgumentCaptor<AiRequest> requests = ArgumentCaptor.forClass(AiRequest.class);
        verify(requestRepository, times(2)).save(requests.capture());
        assertThat(requests.getValue().getStatus()).isEqualTo(AiConstants.STATUS_SUCCESS);
        assertThat(requests.getValue().getModel()).isEqualTo("test-model");

        ArgumentCaptor<AiResponse> responses = ArgumentCaptor.forClass(AiResponse.class);
        verify(responseRepository).save(responses.capture());
        assertThat(responses.getValue().isSuccess()).isTrue();
        assertThat(responses.getValue().getTotalTokens()).isEqualTo(20);
        assertThat(responses.getValue().getLatencyMs()).isEqualTo(55);

        verify(metrics).recordGeneration(eq("MENTOR_REPLY"), anyLong(), eq(12), eq(8));
    }

    @Test
    void recordsFailureTelemetryAndRewrapsProviderException() {
        when(chatProvider.complete(any(ChatRequest.class)))
                .thenThrow(new LlmException("provider exploded"));

        assertThatThrownBy(() -> service.generateJson(userId, "ROADMAP", "sys", "user", Reply.class))
                .isInstanceOf(AiException.class)
                .hasMessageContaining("provider exploded");

        ArgumentCaptor<AiRequest> requests = ArgumentCaptor.forClass(AiRequest.class);
        verify(requestRepository, times(2)).save(requests.capture());
        assertThat(requests.getValue().getStatus()).isEqualTo(AiConstants.STATUS_FAILED);

        ArgumentCaptor<AiResponse> responses = ArgumentCaptor.forClass(AiResponse.class);
        verify(responseRepository).save(responses.capture());
        assertThat(responses.getValue().isSuccess()).isFalse();
        assertThat(responses.getValue().getErrorType()).isEqualTo("LlmException");

        verify(metrics).generationFailure("ROADMAP");
    }

    @Test
    void recordsParsingFailureWhenModelOutputIsNotJson() {
        when(chatProvider.complete(any(ChatRequest.class)))
                .thenReturn(new ChatResult("I refuse to answer in JSON", 3, 4, 7, 10));

        assertThatThrownBy(() -> service.generateJson(userId, "ASSESSMENT", "sys", "user", Reply.class))
                .isInstanceOf(AiException.class);

        // The provider call succeeded, so generation metrics are recorded, then parsing fails.
        verify(metrics).recordGeneration(eq("ASSESSMENT"), anyLong(), eq(3), eq(4));
        verify(metrics).parsingFailure("ASSESSMENT");
    }
}
