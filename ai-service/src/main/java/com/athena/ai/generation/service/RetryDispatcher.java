package com.athena.ai.generation.service;

import com.athena.ai.generation.dto.RetryOutcomeResponse;

import java.util.UUID;

public interface RetryDispatcher {

    RetryOutcomeResponse retryNow(UUID requestId);

    void retryPending();
}
