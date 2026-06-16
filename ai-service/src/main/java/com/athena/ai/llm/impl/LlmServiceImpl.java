package com.athena.ai.llm.impl;

import com.athena.ai.client.AiCompletion;
import com.athena.ai.client.AiException;
import com.athena.ai.client.LmStudioClient;
import com.athena.ai.client.dto.ResponseFormat;
import com.athena.ai.config.AiProperties;
import com.athena.ai.constants.AiConstants;
import com.athena.ai.entity.AiRequest;
import com.athena.ai.entity.AiResponse;
import com.athena.ai.llm.JsonExtractor;
import com.athena.ai.llm.LlmService;
import com.athena.ai.repository.AiRequestRepository;
import com.athena.ai.repository.AiResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmServiceImpl implements LlmService {

    private final LmStudioClient client;
    private final AiRequestRepository requestRepository;
    private final AiResponseRepository responseRepository;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public <T> T generateJson(UUID userId, String promptType, String systemPrompt, String userPrompt, Class<T> type) {
        return generateJson(userId, promptType, systemPrompt, userPrompt, type, ResponseFormat.JSON);
    }

    @Override
    public <T> T generateJson(UUID userId, String promptType, String systemPrompt, String userPrompt,
                              Class<T> type, ResponseFormat responseFormat) {
        AiCompletion completion = invoke(userId, promptType, systemPrompt, userPrompt, responseFormat);
        String raw = completion.content();
        try {
            String json = JsonExtractor.extract(raw);
            return objectMapper.readValue(json, type);
        } catch (RuntimeException ex) {
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

    private AiCompletion invoke(UUID userId, String promptType, String systemPrompt, String userPrompt,
                                ResponseFormat responseFormat) {
        AiRequest request = requestRepository.save(
                new AiRequest(userId, promptType, properties.model(), AiConstants.STATUS_PENDING));
        try {
            AiCompletion completion = client.complete(systemPrompt, userPrompt, responseFormat);
            request.setStatus(AiConstants.STATUS_SUCCESS);
            requestRepository.save(request);
            responseRepository.save(new AiResponse(
                    request.getId(), completion.latencyMs(), completion.totalTokens(), true, null));
            return completion;
        } catch (AiException ex) {
            request.setStatus(AiConstants.STATUS_FAILED);
            requestRepository.save(request);
            responseRepository.save(new AiResponse(request.getId(), 0, 0, false, ex.getClass().getSimpleName()));
            throw ex;
        }
    }
}
