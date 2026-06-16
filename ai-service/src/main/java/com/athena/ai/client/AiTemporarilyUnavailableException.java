package com.athena.ai.client;

import java.util.UUID;

public class AiTemporarilyUnavailableException extends RuntimeException {

    private final transient UUID retryId;

    public AiTemporarilyUnavailableException(UUID retryId) {
        super("Athena is temporarily unavailable. Your progress was saved.");
        this.retryId = retryId;
    }

    public UUID getRetryId() {
        return retryId;
    }
}
