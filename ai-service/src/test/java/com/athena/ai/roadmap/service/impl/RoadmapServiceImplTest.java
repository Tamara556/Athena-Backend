package com.athena.ai.roadmap.service.impl;

import com.athena.ai.generation.model.RoadmapContent;
import com.athena.ai.roadmap.dto.RoadmapResponse;
import com.athena.ai.roadmap.entity.GeneratedRoadmap;
import com.athena.ai.roadmap.repository.GeneratedRoadmapRepository;
import com.athena.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapServiceImplTest {

    @Mock
    private GeneratedRoadmapRepository repository;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private RoadmapServiceImpl service;

    private final UUID userId = UUID.randomUUID();

    private RoadmapServiceImpl service() {
        if (service == null) {
            service = new RoadmapServiceImpl(repository, objectMapper);
        }
        return service;
    }

    @Test
    void getLatestForUserMapsStoredContent() {
        when(repository.findFirstByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(Optional.of(roadmap("CURRENT", "AVAILABLE", "LOCKED")));

        RoadmapResponse response = service().getLatestForUser(userId);

        assertThat(response.goal()).isEqualTo("Learn SQL");
        assertThat(response.phases()).extracting(RoadmapContent.Phase::status)
                .containsExactly("CURRENT", "AVAILABLE", "LOCKED");
    }

    @Test
    void getLatestForUserThrowsWhenAbsent() {
        when(repository.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().getLatestForUser(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByIdThrowsWhenAbsent() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().getById(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void completePhaseAdvancesStatusesAndPersists() {
        when(repository.findFirstByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(Optional.of(roadmap("CURRENT", "AVAILABLE", "LOCKED")));

        RoadmapResponse response = service().completePhase(userId, 0);

        assertThat(response.phases()).extracting(RoadmapContent.Phase::status)
                .containsExactly("COMPLETED", "CURRENT", "AVAILABLE");
        verify(repository).save(any(GeneratedRoadmap.class));
    }

    @Test
    void completePhaseRejectsLockedPhaseAheadOfProgress() {
        when(repository.findFirstByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(Optional.of(roadmap("CURRENT", "AVAILABLE", "LOCKED")));

        assertThatThrownBy(() -> service().completePhase(userId, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("locked");
        verify(repository, never()).save(any());
    }

    @Test
    void completePhaseRejectsOutOfRangeIndex() {
        when(repository.findFirstByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(Optional.of(roadmap("CURRENT", "AVAILABLE", "LOCKED")));

        assertThatThrownBy(() -> service().completePhase(userId, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("out of range");
    }

    private GeneratedRoadmap roadmap(String... statuses) {
        List<RoadmapContent.Phase> phases = java.util.Arrays.stream(statuses)
                .map(s -> new RoadmapContent.Phase("Phase " + s, "desc", 2, List.of("obj"), s))
                .toList();
        String json = objectMapper.writeValueAsString(new RoadmapContent(phases));
        GeneratedRoadmap roadmap = new GeneratedRoadmap(userId, UUID.randomUUID(), "Learn SQL", "BEGINNER", json);
        roadmap.setId(UUID.randomUUID());
        return roadmap;
    }
}
