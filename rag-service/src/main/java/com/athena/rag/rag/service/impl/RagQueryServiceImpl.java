package com.athena.rag.rag.service.impl;

import com.athena.llm.ChatProvider;
import com.athena.llm.model.ChatRequest;
import com.athena.llm.model.ChatResult;
import com.athena.rag.config.RagProperties;
import com.athena.rag.constants.RagConstants;
import com.athena.rag.observability.RagMetrics;
import com.athena.rag.rag.dto.RagAnswerResponse;
import com.athena.rag.rag.dto.RagQueryRequest;
import com.athena.rag.rag.entity.RagQueryLog;
import com.athena.rag.rag.repository.RagQueryLogRepository;
import com.athena.rag.rag.service.AssembledContext;
import com.athena.rag.rag.service.ContextAssembler;
import com.athena.rag.rag.service.GroundingPolicy;
import com.athena.rag.rag.service.PromptBuilder;
import com.athena.rag.rag.service.RagQueryService;
import com.athena.rag.retrieval.domain.RetrievedChunk;
import com.athena.rag.retrieval.service.RetrievalRequest;
import com.athena.rag.retrieval.service.RetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagQueryServiceImpl implements RagQueryService {

    private static final String UNGROUNDED_MESSAGE = """
            I don't have enough of your learning history yet to answer that with confidence. \
            As you complete more lessons, interviews and reflections, Athena's memory of your journey \
            grows and I'll be able to answer questions like this.""";

    private final RetrievalService retrievalService;
    private final ContextAssembler contextAssembler;
    private final PromptBuilder promptBuilder;
    private final GroundingPolicy groundingPolicy;
    private final ChatProvider chatProvider;
    private final RagQueryLogRepository queryLogRepository;
    private final RagMetrics metrics;
    private final RagProperties properties;

    @Override
    public RagAnswerResponse answer(UUID userId, RagQueryRequest request) {
        long startNanos = System.nanoTime();
        int topK = request.topK() == null ? properties.retrievalTopK()
                : Math.min(request.topK(), properties.maxSearchResults());

        List<RetrievedChunk> chunks = retrievalService.retrieve(new RetrievalRequest(
                userId, request.query(), request.sourceTypes(), request.domain(),
                topK, properties.minSimilarity(), true, 0));

        if (!groundingPolicy.isGrounded(chunks)) {
            long latencyMs = elapsedMs(startNanos);
            metrics.ungrounded();
            metrics.recordQueryLatency(latencyMs);
            persist(userId, request.query(), chunks.size(), null, false,
                    latencyMs, 0, 0, RagConstants.STATUS_UNGROUNDED);
            log.info("rag query ungrounded userId={} retrieved={}", userId, chunks.size());
            return new RagAnswerResponse(UNGROUNDED_MESSAGE, false, 0, 0.0, List.of());
        }

        AssembledContext context = contextAssembler.assemble(chunks);
        ChatResult result;
        try {
            result = chatProvider.complete(ChatRequest.of(
                    promptBuilder.system(), promptBuilder.user(request.query(), context.contextText())));
        } catch (RuntimeException ex) {
            long latencyMs = elapsedMs(startNanos);
            metrics.llmFailure();
            persist(userId, request.query(), chunks.size(), context.topScore(), true,
                    latencyMs, 0, 0, RagConstants.STATUS_FAILED);
            throw ex;
        }

        long latencyMs = elapsedMs(startNanos);
        metrics.recordQueryLatency(latencyMs);
        persist(userId, request.query(), chunks.size(), context.topScore(), true,
                latencyMs, result.promptTokens(), result.completionTokens(), RagConstants.STATUS_SUCCESS);
        log.info("rag query grounded userId={} retrieved={} used={} topScore={} latencyMs={} totalTokens={}",
                userId, chunks.size(), context.usedCount(), context.topScore(), latencyMs, result.totalTokens());

        return new RagAnswerResponse(result.content().strip(), true,
                context.usedCount(), context.topScore(), context.citations());
    }

    private void persist(UUID userId, String query, int retrievedCount, Double topScore, boolean grounded,
                         long latencyMs, int promptTokens, int completionTokens, String status) {
        queryLogRepository.save(new RagQueryLog(userId, query, retrievedCount, topScore, grounded,
                chatProvider.model(), latencyMs, promptTokens, completionTokens, status));
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
