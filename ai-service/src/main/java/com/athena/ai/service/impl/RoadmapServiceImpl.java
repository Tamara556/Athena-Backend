package com.athena.ai.service.impl;

import com.athena.ai.constants.AiConstants;
import com.athena.ai.dto.RoadmapResponse;
import com.athena.ai.entity.GeneratedRoadmap;
import com.athena.ai.model.RoadmapContent;
import com.athena.ai.repository.GeneratedRoadmapRepository;
import com.athena.ai.service.RoadmapService;
import com.athena.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoadmapServiceImpl implements RoadmapService {

    private final GeneratedRoadmapRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public RoadmapResponse getLatestForUser(UUID userId) {
        return toResponse(repository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> ResourceNotFoundException.of(AiConstants.RESOURCE_ROADMAP, userId)));
    }

    @Override
    @Transactional(readOnly = true)
    public RoadmapResponse getById(UUID id) {
        return toResponse(repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of(AiConstants.RESOURCE_ROADMAP, id)));
    }

    private RoadmapResponse toResponse(GeneratedRoadmap roadmap) {
        RoadmapContent content = objectMapper.readValue(roadmap.getContentJson(), RoadmapContent.class);
        return new RoadmapResponse(roadmap.getId(), roadmap.getGoal(), roadmap.getLevel(),
                content.phases(), roadmap.getCreatedAt());
    }
}
