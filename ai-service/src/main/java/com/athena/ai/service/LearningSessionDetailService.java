package com.athena.ai.service;

import com.athena.ai.dto.LearningSessionResponse;

import java.util.UUID;

public interface LearningSessionDetailService {

    LearningSessionResponse getDetail(UUID sessionId);

    void evict(UUID sessionId);
}
