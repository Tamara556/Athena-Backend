package com.athena.ai.roadmap.service.impl;

import com.athena.ai.constants.AiConstants;
import com.athena.ai.roadmap.dto.RoadmapResponse;
import com.athena.ai.roadmap.entity.GeneratedRoadmap;
import com.athena.ai.generation.model.RoadmapContent;
import com.athena.ai.roadmap.repository.GeneratedRoadmapRepository;
import com.athena.ai.roadmap.service.RoadmapService;
import com.athena.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoadmapServiceImpl implements RoadmapService {

    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CURRENT = "CURRENT";
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_LOCKED = "LOCKED";

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

    @Override
    @Transactional
    public RoadmapResponse completePhase(UUID userId, int phaseIndex) {
        GeneratedRoadmap roadmap = repository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> ResourceNotFoundException.of(AiConstants.RESOURCE_ROADMAP, userId));

        RoadmapContent content = objectMapper.readValue(roadmap.getContentJson(), RoadmapContent.class);
        List<RoadmapContent.Phase> phases = content.phases();
        if (phases == null || phases.isEmpty() || phaseIndex < 0 || phaseIndex >= phases.size()) {
            throw new IllegalArgumentException("Phase index out of range: " + phaseIndex);
        }

        int completedCount = (int) phases.stream()
                .filter(p -> STATUS_COMPLETED.equalsIgnoreCase(p.status()))
                .count();
        if (phaseIndex > completedCount) {
            throw new IllegalArgumentException(
                    "Phase " + (phaseIndex + 1) + " is locked and cannot be completed yet");
        }

        List<RoadmapContent.Phase> updated = recompute(phases, Math.max(completedCount, phaseIndex + 1));
        roadmap.setContentJson(objectMapper.writeValueAsString(new RoadmapContent(updated)));
        repository.save(roadmap);

        return new RoadmapResponse(roadmap.getId(), roadmap.getGoal(), roadmap.getLevel(),
                updated, roadmap.getCreatedAt());
    }

    private List<RoadmapContent.Phase> recompute(List<RoadmapContent.Phase> phases, int completedCount) {
        List<RoadmapContent.Phase> result = new ArrayList<>(phases.size());
        for (int i = 0; i < phases.size(); i++) {
            RoadmapContent.Phase p = phases.get(i);
            String status = i < completedCount ? STATUS_COMPLETED
                    : i == completedCount ? STATUS_CURRENT
                    : i == completedCount + 1 ? STATUS_AVAILABLE
                    : STATUS_LOCKED;
            result.add(new RoadmapContent.Phase(
                    p.name(), p.description(), p.durationWeeks(), p.objectives(), status));
        }
        return result;
    }

    private RoadmapResponse toResponse(GeneratedRoadmap roadmap) {
        RoadmapContent content = objectMapper.readValue(roadmap.getContentJson(), RoadmapContent.class);
        return new RoadmapResponse(roadmap.getId(), roadmap.getGoal(), roadmap.getLevel(),
                content.phases(), roadmap.getCreatedAt());
    }
}
