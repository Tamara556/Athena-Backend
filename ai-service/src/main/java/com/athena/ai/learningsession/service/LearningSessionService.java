package com.athena.ai.learningsession.service;

import com.athena.ai.learningsession.dto.LearningSessionResponse;
import com.athena.ai.learningsession.dto.LearningSessionSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface LearningSessionService {

    void generateInitialBuffer(UUID userId, UUID roadmapId);

    void refillBuffer(UUID userId);

    LearningSessionResponse prepareCurrent(UUID userId);

    LearningSessionResponse getCurrent(UUID userId);

    LearningSessionResponse getById(UUID userId, UUID sessionId);

    LearningSessionResponse start(UUID userId, UUID sessionId);

    LearningSessionResponse complete(UUID userId, UUID sessionId);

    List<LearningSessionSummaryResponse> getUpcoming(UUID userId);
}
