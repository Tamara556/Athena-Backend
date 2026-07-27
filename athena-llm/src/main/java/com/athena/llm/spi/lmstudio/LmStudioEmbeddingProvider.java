package com.athena.llm.spi.lmstudio;

import com.athena.llm.EmbeddingProvider;
import com.athena.llm.LlmException;
import com.athena.llm.LlmUnavailableException;
import com.athena.llm.config.LlmProviderProperties;
import com.athena.llm.model.EmbeddingResult;
import com.athena.llm.spi.lmstudio.dto.LmEmbeddingRequest;
import com.athena.llm.spi.lmstudio.dto.LmEmbeddingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class LmStudioEmbeddingProvider implements EmbeddingProvider {

    private static final String NAME = "lm-studio";

    private final WebClient webClient;
    private final LlmProviderProperties.Embedding properties;

    public LmStudioEmbeddingProvider(WebClient embeddingWebClient, LlmProviderProperties.Embedding properties) {
        this.webClient = embeddingWebClient;
        this.properties = properties;
    }

    @Override
    public EmbeddingResult embed(List<String> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw new LlmException("Embedding input must not be empty");
        }
        long startNanos = System.nanoTime();
        List<float[]> vectors = new ArrayList<>(inputs.size());
        int totalTokens = 0;

        for (int from = 0; from < inputs.size(); from += properties.batchSize()) {
            int to = Math.min(from + properties.batchSize(), inputs.size());
            LmEmbeddingResponse response = call(inputs.subList(from, to));
            totalTokens += response.totalTokens();
            response.data().stream()
                    .sorted(Comparator.comparingInt(LmEmbeddingResponse.Data::index))
                    .forEach(d -> vectors.add(validated(d.embedding())));
        }

        long latencyMs = elapsedMs(startNanos);
        log.info("embedding ok provider={} model={} count={} dimension={} latencyMs={} totalTokens={}",
                NAME, properties.model(), vectors.size(), properties.dimension(), latencyMs, totalTokens);
        return new EmbeddingResult(vectors, properties.dimension(), totalTokens, latencyMs);
    }

    @Override
    public float[] embedOne(String input) {
        return embed(List.of(input)).vectors().getFirst();
    }

    @Override
    public int dimension() {
        return properties.dimension();
    }

    @Override
    public String model() {
        return properties.model();
    }

    @Override
    public String name() {
        return NAME;
    }

    private LmEmbeddingResponse call(List<String> batch) {
        long startNanos = System.nanoTime();
        try {
            LmEmbeddingResponse response = webClient.post()
                    .uri("/embeddings")
                    .bodyValue(new LmEmbeddingRequest(properties.model(), batch))
                    .retrieve()
                    .bodyToMono(LmEmbeddingResponse.class)
                    .retryWhen(Retry.backoff(properties.maxRetries(), properties.retryBackoff())
                            .filter(this::isTransient)
                            .doBeforeRetry(s -> log.warn("Retrying embedding attempt={} model={}",
                                    s.totalRetries() + 1, properties.model())))
                    .block();
            if (response == null || response.data() == null || response.data().isEmpty()) {
                throw new LlmException("LM Studio returned no embedding data");
            }
            if (response.data().size() != batch.size()) {
                throw new LlmException("LM Studio returned " + response.data().size()
                        + " embeddings for " + batch.size() + " inputs");
            }
            return response;
        } catch (LlmException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("embedding failed provider={} model={} latencyMs={} cause={}",
                    NAME, properties.model(), elapsedMs(startNanos), ex.getClass().getSimpleName());
            if (isTransient(ex)) {
                throw new LlmUnavailableException("LM Studio embedding unavailable: " + ex.getMessage(), ex);
            }
            throw new LlmException("LM Studio embedding request failed: " + ex.getMessage(), ex);
        }
    }

    private float[] validated(float[] embedding) {
        if (embedding == null || embedding.length != properties.dimension()) {
            throw new LlmException("Embedding dimension mismatch: expected " + properties.dimension()
                    + " got " + (embedding == null ? 0 : embedding.length));
        }
        return embedding;
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
