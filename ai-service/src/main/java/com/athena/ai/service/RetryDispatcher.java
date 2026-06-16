package com.athena.ai.service;

import com.athena.ai.dto.RetryOutcomeResponse;

import java.util.UUID;

public interface RetryDispatcher {

    RetryOutcomeResponse retryNow(UUID requestId);

    void retryPending();
}
