package com.athena.rag.recommendation.service.impl;

import com.athena.llm.ChatProvider;
import com.athena.llm.LlmException;
import com.athena.llm.model.ChatMessage;
import com.athena.llm.model.ChatRequest;
import com.athena.llm.model.ChatResult;
import com.athena.llm.model.ResponseFormat;
import com.athena.rag.client.KnowledgeGraphClient;
import com.athena.rag.client.ProgressClient;
import com.athena.rag.config.RagProperties;
import com.athena.rag.memory.ingestion.JsonText;
import com.athena.rag.observability.RagMetrics;
import com.athena.rag.rag.service.AssembledContext;
import com.athena.rag.rag.service.ContextAssembler;
import com.athena.rag.recommendation.dto.NextRecommendationRequest;
import com.athena.rag.recommendation.dto.NextRecommendationResponse;
import com.athena.rag.recommendation.model.NextRecommendationContent;
import com.athena.rag.recommendation.service.RecommendationService;
import com.athena.rag.retrieval.domain.RetrievedChunk;
import com.athena.rag.retrieval.service.RetrievalRequest;
import com.athena.rag.retrieval.service.RetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private static final String SYSTEM_PROMPT = """
            You are Athena, a personal learning mentor. Using ONLY the learner's own CONTEXT and SIGNALS,
            decide what this specific learner should focus on next. Base every judgement on their real
            history: weak areas, interview results, roadmap phases and completed work. Do not invent facts.
            If a learning domain is given, keep the recommendation within that domain.
            Return a single concrete next focus, a short rationale grounded in the learner's history,
            and two to four focus areas.""";

    private static final Map<String, Object> SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "recommendation", Map.of("type", "string"),
                    "rationale", Map.of("type", "string"),
                    "focusAreas", Map.of("type", "array", "items", Map.of("type", "string"))),
            "required", List.of("recommendation", "rationale", "focusAreas"));

    private final RetrievalService retrievalService;
    private final ContextAssembler contextAssembler;
    private final ChatProvider chatProvider;
    private final KnowledgeGraphClient knowledgeGraphClient;
    private final ProgressClient progressClient;
    private final RagProperties properties;
    private final RagMetrics metrics;
    private final ObjectMapper objectMapper;

    @Override
    public NextRecommendationResponse recommendNext(UUID userId, NextRecommendationRequest request) {
        String domain = request == null ? null : request.domain();
        String query = "Based on my learning history, what should I focus on learning next"
                + (domain == null || domain.isBlank() ? "" : " in " + domain) + "?";

        List<RetrievedChunk> chunks = retrievalService.retrieve(new RetrievalRequest(
                userId, query, null, domain, properties.retrievalTopK(), properties.minSimilarity(), true, 0));
        String knowledgeGraph = safe(() -> JsonText.flatten(knowledgeGraphClient.getGraph(userId)));
        String progress = safe(() -> JsonText.flatten(progressClient.getProgress(userId)));

        boolean hasSignals = !chunks.isEmpty() || isPresent(knowledgeGraph) || isPresent(progress);
        if (!hasSignals) {
            metrics.ungrounded();
            return new NextRecommendationResponse(
                    "I don't have enough of your learning history yet to recommend a next step. "
                            + "Complete a lesson or an interview and ask again.",
                    "No learning signals are available for this learner yet.", List.of(), false, List.of());
        }

        AssembledContext context = contextAssembler.assemble(chunks);
        String userPrompt = buildPrompt(context.contextText(), knowledgeGraph, progress, domain);

        ChatResult result;
        try {
            result = chatProvider.complete(new ChatRequest(
                    List.of(ChatMessage.system(SYSTEM_PROMPT), ChatMessage.user(userPrompt)),
                    ResponseFormat.ofSchema("next_recommendation", SCHEMA), null, null));
        } catch (RuntimeException ex) {
            metrics.llmFailure();
            throw ex;
        }

        NextRecommendationContent content = parse(result.content());
        log.info("recommendation userId={} retrieved={} totalTokens={}", userId, chunks.size(), result.totalTokens());
        return new NextRecommendationResponse(content.recommendation(), content.rationale(),
                content.focusAreas() == null ? List.of() : content.focusAreas(), true, context.citations());
    }

    private String buildPrompt(String contextText, String knowledgeGraph, String progress, String domain) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("CONTEXT:\n").append(contextText.isBlank() ? "(no retrieved lessons)" : contextText);
        prompt.append("\n\nSIGNALS:\n");
        prompt.append("Knowledge graph: ").append(isPresent(knowledgeGraph) ? knowledgeGraph : "unavailable").append('\n');
        prompt.append("Progress: ").append(isPresent(progress) ? progress : "unavailable").append('\n');
        if (domain != null && !domain.isBlank()) {
            prompt.append("\nTarget domain: ").append(domain);
        }
        return prompt.toString();
    }

    private NextRecommendationContent parse(String raw) {
        String json = extractJson(raw);
        try {
            return objectMapper.readValue(json, NextRecommendationContent.class);
        } catch (RuntimeException ex) {
            metrics.llmFailure();
            throw new LlmException("Could not parse recommendation output", ex);
        }
    }

    private String extractJson(String raw) {
        if (raw == null) {
            throw new LlmException("Empty recommendation output");
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new LlmException("Recommendation output did not contain a JSON object");
        }
        return raw.substring(start, end + 1);
    }

    private String safe(Supplier<String> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException ex) {
            log.warn("Recommendation signal unavailable cause={}", ex.getClass().getSimpleName());
            return null;
        }
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
