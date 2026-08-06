package com.athena.ai.roadmap.service;

import com.athena.ai.roadmap.dto.RoadmapResponse;

import java.util.UUID;

public interface RoadmapService {

    RoadmapResponse getLatestForUser(UUID userId);

    RoadmapResponse getById(UUID id);

    RoadmapResponse completePhase(UUID userId, int phaseIndex);
}
