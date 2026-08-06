package com.athena.ai.generation.observability;

import com.athena.ai.constants.AiConstants;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AiMetrics {

    private final MeterRegistry registry;

    public AiMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordGeneration(String promptType, long latencyMs, int promptTokens, int completionTokens) {
        registry.timer(AiConstants.METRIC_LLM_LATENCY, AiConstants.METRIC_TAG_PROMPT, promptType)
                .record(Duration.ofMillis(latencyMs));
        registry.counter(AiConstants.METRIC_LLM_REQUESTS, AiConstants.METRIC_TAG_PROMPT, promptType).increment();
        registry.summary(AiConstants.METRIC_LLM_PROMPT_TOKENS, AiConstants.METRIC_TAG_PROMPT, promptType)
                .record(promptTokens);
        registry.summary(AiConstants.METRIC_LLM_COMPLETION_TOKENS, AiConstants.METRIC_TAG_PROMPT, promptType)
                .record(completionTokens);
    }

    public void generationFailure(String promptType) {
        registry.counter(AiConstants.METRIC_LLM_FAILURES, AiConstants.METRIC_TAG_PROMPT, promptType).increment();
    }

    public void parsingFailure(String promptType) {
        registry.counter(AiConstants.METRIC_LLM_PARSE_FAILURES, AiConstants.METRIC_TAG_PROMPT, promptType).increment();
    }
}
