package com.athena.rag.observability;

import com.athena.rag.constants.RagConstants;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RagMetrics {

    private final MeterRegistry registry;

    public RagMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordEmbeddingLatency(long millis) {
        registry.timer(RagConstants.METRIC_EMBED_LATENCY).record(Duration.ofMillis(millis));
    }

    public void embeddingFailure() {
        registry.counter(RagConstants.METRIC_EMBED_FAILURES).increment();
    }

    public void recordRetrieval(long millis, int documentCount, double topScore) {
        registry.timer(RagConstants.METRIC_RETRIEVAL_LATENCY).record(Duration.ofMillis(millis));
        registry.summary(RagConstants.METRIC_RETRIEVED_DOCS).record(documentCount);
        registry.summary(RagConstants.METRIC_TOP_SCORE).record(topScore);
    }

    public void retrievalFailure() {
        registry.counter(RagConstants.METRIC_RETRIEVAL_FAILURES).increment();
    }

    public void recordQueryLatency(long millis) {
        registry.timer(RagConstants.METRIC_QUERY_LATENCY).record(Duration.ofMillis(millis));
    }

    public void ungrounded() {
        registry.counter(RagConstants.METRIC_QUERY_UNGROUNDED).increment();
    }

    public void llmFailure() {
        registry.counter(RagConstants.METRIC_LLM_FAILURES).increment();
    }
}
