package com.athena.ai.roadmap.service;

import com.athena.ai.roadmap.dto.RoadmapResponse;

import java.util.UUID;

public interface RoadmapService {

    RoadmapResponse getLatestForUser(UUID userId);

    RoadmapResponse getById(UUID id);

    /**
     * Same as {@link #getById(UUID)} but rejects a caller who does not own the
     * roadmap. Used for gateway-authenticated requests that carry {@code X-User-Id}.
     */
    RoadmapResponse getByIdForUser(UUID id, UUID requesterId);

    RoadmapResponse completePhase(UUID userId, int phaseIndex);
}
