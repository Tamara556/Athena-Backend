package com.athena.ai.llm.impl;

import com.athena.ai.constants.AiConstants;
import com.athena.ai.generation.entity.AiRequest;
import com.athena.ai.generation.entity.AiResponse;
import com.athena.ai.generation.observability.AiMetrics;
import com.athena.ai.generation.repository.AiRequestRepository;
import com.athena.ai.generation.repository.AiResponseRepository;
import com.athena.ai.llm.AiException;
import com.athena.ai.llm.LlmService;
import com.athena.llm.ChatProvider;
import com.athena.llm.LlmException;
import com.athena.llm.model.ChatRequest;
import com.athena.llm.model.ChatResult;
import com.athena.llm.model.ResponseFormat;
import com.athena.llm.parser.StructuredOutputParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmServiceImpl implements LlmService {

    private static final ResponseFormat DEFAULT_JSON =
            ResponseFormat.ofSchema("athena_response", Map.of("type", "object"));

    private final ChatProvider chatProvider;
    private final AiRequestRepository requestRepository;
    private final AiResponseRepository responseRepository;
    private final AiMetrics metrics;
    private final ObjectMapper objectMapper;

    @Override
    public <T> T generateJson(UUID userId, String promptType, String systemPrompt, String userPrompt, Class<T> type) {
        return generateJson(userId, promptType, systemPrompt, userPrompt, type, DEFAULT_JSON);
    }

    @Override
    public <T> T generateJson(UUID userId, String promptType, String systemPrompt, String userPrompt,
                              Class<T> type, ResponseFormat responseFormat) {
        ChatResult result = invoke(userId, promptType, systemPrompt, userPrompt, responseFormat);
        String raw = result.content();
        try {
            String json = StructuredOutputParser.extract(raw);
            return objectMapper.readValue(json, type);
        } catch (RuntimeException ex) {
            metrics.parsingFailure(promptType);
            log.warn("Failed to parse AI JSON promptType={} cause={} rawLen={} head=[{}] tail=[{}]",
                    promptType, ex.getClass().getSimpleName(),
                    raw == null ? 0 : raw.length(), snippet(raw, 0, 300), snippet(raw, -200, 200));
            throw new AiException("Could not parse AI JSON for " + promptType, ex);
        }
    }

    private static String snippet(String text, int from, int max) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String oneLine = text.replaceAll("\\s+", " ").trim();
        if (from < 0) {
            int start = Math.max(0, oneLine.length() - max);
            return oneLine.substring(start);
        }
        return oneLine.length() <= max ? oneLine : oneLine.substring(0, max) + "…";
    }

    private ChatResult invoke(UUID userId, String promptType, String systemPrompt, String userPrompt,
                              ResponseFormat responseFormat) {
        AiRequest request = requestRepository.save(
                new AiRequest(userId, promptType, chatProvider.model(), AiConstants.STATUS_PENDING));
        try {
            ChatResult result = chatProvider.complete(
                    ChatRequest.of(systemPrompt, userPrompt, responseFormat));
            request.setStatus(AiConstants.STATUS_SUCCESS);
            requestRepository.save(request);
            responseRepository.save(new AiResponse(
                    request.getId(), result.latencyMs(), result.totalTokens(), true, null));
            metrics.recordGeneration(promptType, result.latencyMs(),
                    result.promptTokens(), result.completionTokens());
            return result;
        } catch (LlmException ex) {
            request.setStatus(AiConstants.STATUS_FAILED);
            requestRepository.save(request);
            responseRepository.save(new AiResponse(request.getId(), 0, 0, false, ex.getClass().getSimpleName()));
            metrics.generationFailure(promptType);
            throw new AiException(ex.getMessage(), ex);
        }
    }
}
