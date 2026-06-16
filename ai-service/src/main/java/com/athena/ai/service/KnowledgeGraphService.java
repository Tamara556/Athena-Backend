package com.athena.ai.service;

import com.athena.ai.dto.KnowledgeNodeResponse;
import com.athena.ai.dto.UpdateKnowledgeRequest;

import java.util.List;
import java.util.UUID;

public interface KnowledgeGraphService {

    List<KnowledgeNodeResponse> getForUser(UUID userId);

    List<KnowledgeNodeResponse> applyUpdates(UUID userId, UpdateKnowledgeRequest request);

    void seedSkills(UUID userId, String domain, List<String> skills);

    void recordWeaknesses(UUID userId, String domain, List<String> weaknesses);
}
