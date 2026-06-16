package com.athena.ai.service;

import com.athena.ai.entity.AiRequestRetry;

import java.util.List;
import java.util.UUID;

public interface AiRetryService {

    AiRequestRetry record(String requestType, UUID userId, String payloadReference);

    List<AiRequestRetry> findRetryable();

    void markProcessing(AiRequestRetry retry);

    void markCompleted(AiRequestRetry retry);

    void markRetryableFailure(AiRequestRetry retry);
}
