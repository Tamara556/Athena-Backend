package com.athena.llm.spi.lmstudio;

import com.athena.llm.ChatProvider;
import com.athena.llm.LlmException;
import com.athena.llm.LlmUnavailableException;
import com.athena.llm.config.LlmProviderProperties;
import com.athena.llm.model.ChatRequest;
import com.athena.llm.model.ChatResult;
import com.athena.llm.model.ResponseFormat;
import com.athena.llm.spi.lmstudio.dto.LmChatRequest;
import com.athena.llm.spi.lmstudio.dto.LmChatResponse;
import com.athena.llm.spi.lmstudio.dto.LmResponseFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

@Slf4j
public class LmStudioChatProvider implements ChatProvider {

    private static final String NAME = "lm-studio";

    private final WebClient webClient;
    private final LlmProviderProperties.Chat properties;

    public LmStudioChatProvider(WebClient chatWebClient, LlmProviderProperties.Chat properties) {
        this.webClient = chatWebClient;
        this.properties = properties;
    }

    @Override
    public ChatResult complete(ChatRequest request) {
        LmChatRequest body = new LmChatRequest(
                properties.model(),
                request.messages().stream()
                        .map(m -> new LmChatRequest.LmChatMessage(m.role(), m.content()))
                        .toList(),
                request.temperature() == null ? properties.temperature() : request.temperature(),
                request.maxTokens() == null ? properties.maxTokens() : request.maxTokens(),
                false,
                toLmFormat(request.responseFormat()));

        long startNanos = System.nanoTime();
        try {
            LmChatResponse response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(LmChatResponse.class)
                    .retryWhen(Retry.backoff(properties.maxRetries(), properties.retryBackoff())
                            .filter(this::isTransient)
                            .doBeforeRetry(s -> log.warn("Retrying chat completion attempt={} model={}",
                                    s.totalRetries() + 1, properties.model())))
                    .block();

            long latencyMs = elapsedMs(startNanos);
            if (response == null || !response.hasContent()) {
                throw new LlmException("LM Studio returned an empty chat completion");
            }
            log.info("chat completion provider={} model={} latencyMs={} totalTokens={}",
                    NAME, properties.model(), latencyMs, response.totalTokens());
            return new ChatResult(response.firstContent(), response.promptTokens(),
                    response.completionTokens(), response.totalTokens(), latencyMs);
        } catch (LlmException ex) {
            throw ex;
        } catch (Exception ex) {
            long latencyMs = elapsedMs(startNanos);
            log.error("chat completion failed provider={} model={} latencyMs={} cause={}",
                    NAME, properties.model(), latencyMs, ex.getClass().getSimpleName());
            if (isTransient(ex)) {
                throw new LlmUnavailableException("LM Studio chat unavailable: " + ex.getMessage(), ex);
            }
            throw new LlmException("LM Studio chat request failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String model() {
        return properties.model();
    }

    @Override
    public String name() {
        return NAME;
    }

    private LmResponseFormat toLmFormat(ResponseFormat responseFormat) {
        if (responseFormat == null) {
            return LmResponseFormat.text();
        }
        return switch (responseFormat.kind()) {
            case TEXT -> LmResponseFormat.text();
            case JSON -> LmResponseFormat.json();
            case JSON_SCHEMA -> LmResponseFormat.schema(responseFormat.schemaName(), responseFormat.schema());
        };
    }

    private boolean isTransient(Throwable ex) {
        if (ex instanceof WebClientRequestException) {
            return true;
        }
        return ex instanceof WebClientResponseException wcre && wcre.getStatusCode().is5xxServerError();
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
