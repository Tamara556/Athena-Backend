package com.athena.ai.learningsession.service;

import com.athena.ai.learningsession.dto.LearningSessionResponse;

import java.util.UUID;

public interface LearningSessionDetailService {

    LearningSessionResponse getDetail(UUID sessionId);

    void evict(UUID sessionId);
}
