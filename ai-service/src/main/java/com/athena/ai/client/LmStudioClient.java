package com.athena.ai.client;

import com.athena.ai.client.dto.ChatCompletionRequest;
import com.athena.ai.client.dto.ChatCompletionResponse;
import com.athena.ai.client.dto.ChatMessage;
import com.athena.ai.client.dto.ResponseFormat;
import com.athena.ai.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.util.List;

@Slf4j
@Component
public class LmStudioClient {

    private final WebClient webClient;
    private final AiProperties properties;

    public LmStudioClient(WebClient lmStudioWebClient, AiProperties properties) {
        this.webClient = lmStudioWebClient;
        this.properties = properties;
    }

    public AiCompletion complete(String systemPrompt, String userPrompt, ResponseFormat responseFormat) {
        ChatCompletionRequest request = new ChatCompletionRequest(
                properties.model(),
                List.of(ChatMessage.system(systemPrompt), ChatMessage.user(userPrompt)),
                properties.temperature(),
                properties.maxTokens(),
                false,
                responseFormat);

        long startNanos = System.nanoTime();
        try {
            ChatCompletionResponse response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatCompletionResponse.class)
                    .retryWhen(Retry.backoff(properties.maxRetries(), properties.retryBackoff())
                            .filter(this::isTransient)
                            .doBeforeRetry(s -> log.warn("Retrying LM call (attempt {}) model={}",
                                    s.totalRetries() + 1, properties.model())))
                    .block();

            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;

            if (response == null || !response.hasContent()) {
                throw new AiException("LM Studio returned an empty completion");
            }
            log.info("LM completion ok model={} latencyMs={} tokens={}",
                    properties.model(), latencyMs, response.totalTokens());
            return new AiCompletion(response.firstContent(), response.totalTokens(), latencyMs);

        } catch (AiException ex) {
            throw ex;
        } catch (Exception ex) {
            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.error("LM call failed model={} latencyMs={} cause={}",
                    properties.model(), latencyMs, ex.getClass().getSimpleName());
            throw new AiException("LM Studio request failed: " + ex.getMessage(), ex);
        }
    }

    private boolean isTransient(Throwable ex) {
        if (ex instanceof WebClientRequestException) {
            return true;
        }
        return ex instanceof WebClientResponseException wcre && wcre.getStatusCode().is5xxServerError();
    }
}
