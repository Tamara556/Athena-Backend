package com.athena.ai.service;

import com.athena.ai.dto.RoadmapResponse;

import java.util.UUID;

public interface RoadmapService {

    RoadmapResponse getLatestForUser(UUID userId);

    RoadmapResponse getById(UUID id);
}
