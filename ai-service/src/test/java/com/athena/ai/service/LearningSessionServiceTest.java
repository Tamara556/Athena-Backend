package com.athena.ai.service;

import com.athena.ai.domain.SessionStatus;
import com.athena.ai.entity.GeneratedRoadmap;
import com.athena.ai.entity.LearningSession;
import com.athena.ai.entity.OnboardingSession;
import com.athena.ai.messaging.AiEventPublisher;
import com.athena.ai.model.RoadmapContent;
import com.athena.ai.repository.GeneratedRoadmapRepository;
import com.athena.ai.repository.LearningSessionRepository;
import com.athena.ai.repository.OnboardingSessionRepository;
import com.athena.ai.service.impl.LearningSessionGenerator;
import com.athena.ai.service.impl.LearningSessionServiceImpl;
import com.athena.common.event.KafkaTopics;
import com.athena.common.event.LearningSessionCompletedEvent;
import com.athena.common.event.NodeBufferRefilledEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningSessionServiceTest {

    @Mock private LearningSessionRepository sessionRepository;
    @Mock private GeneratedRoadmapRepository roadmapRepository;
    @Mock private OnboardingSessionRepository onboardingRepository;
    @Mock private LearningSessionGenerator generator;
    @Mock private LearningSessionDetailService detail;
    @Mock private AiEventPublisher events;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-17T00:00:00Z"), ZoneOffset.UTC);

    private LearningSessionService service;

    private final UUID userId = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private final UUID roadmapId = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @BeforeEach
    void setUp() {
        service = new LearningSessionServiceImpl(sessionRepository, roadmapRepository, onboardingRepository,
                generator, detail, events, objectMapper, clock);
    }

    private GeneratedRoadmap roadmapWithPhases(int phaseCount) {
        List<RoadmapContent.Phase> phases = IntStream.range(0, phaseCount)
                .mapToObj(i -> new RoadmapContent.Phase("Phase " + i, "desc", 1, List.of("objective"), "LOCKED"))
                .toList();
        GeneratedRoadmap roadmap = new GeneratedRoadmap(
                userId, UUID.randomUUID(), "Learn Java", "beginner",
                objectMapper.writeValueAsString(new RoadmapContent(phases)));
        roadmap.setId(roadmapId);
        return roadmap;
    }

    private LearningSession session(int nodeIndex, SessionStatus status) {
        LearningSession s = new LearningSession(userId, roadmapId,
                LearningSessionGenerator.nodeId(roadmapId, nodeIndex), nodeIndex, "Phase " + nodeIndex, 60);
        s.setId(UUID.randomUUID());
        s.setStatus(status);
        return s;
    }

    @Test
    void generateInitialBuffer_generatesExactlyFiveForLongRoadmap() {
        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmapWithPhases(7)));
        when(onboardingRepository.findFirstByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(Optional.of(onboardingWithDomain("Programming")));
        when(generator.generate(any(), any(), any(), anyInt(), any())).thenReturn(session(0, SessionStatus.NOT_STARTED));

        service.generateInitialBuffer(userId, roadmapId);

        verify(generator, times(5)).generate(eq(userId), any(GeneratedRoadmap.class), any(RoadmapContent.class),
                anyInt(), eq("Programming"));
    }

    @Test
    void generateInitialBuffer_capsAtRoadmapSize() {
        when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmapWithPhases(3)));
        when(onboardingRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.empty());
        when(generator.generate(any(), any(), any(), anyInt(), any())).thenReturn(session(0, SessionStatus.NOT_STARTED));

        service.generateInitialBuffer(userId, roadmapId);

        verify(generator, times(3)).generate(eq(userId), any(), any(), anyInt(), eq("General"));
    }

    @Test
    void refillBuffer_generatesNextMissingNodeAndPublishesEvent() {
        when(roadmapRepository.findFirstByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(Optional.of(roadmapWithPhases(7)));
        when(onboardingRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.empty());
        when(sessionRepository.findByUserIdAndRoadmapIdOrderByNodeIndexAsc(userId, roadmapId)).thenReturn(List.of(
                session(0, SessionStatus.COMPLETED), session(1, SessionStatus.NOT_STARTED),
                session(2, SessionStatus.NOT_STARTED), session(3, SessionStatus.NOT_STARTED),
                session(4, SessionStatus.NOT_STARTED)));
        when(generator.generate(any(), any(), any(), eq(5), any())).thenReturn(session(5, SessionStatus.NOT_STARTED));

        service.refillBuffer(userId);

        verify(generator).generate(eq(userId), any(), any(), eq(5), eq("General"));
        verify(events).publish(eq(KafkaTopics.NODE_BUFFER_REFILLED), eq(userId), any(NodeBufferRefilledEvent.class));
    }

    @Test
    void refillBuffer_doesNothingWhenBufferComplete() {
        when(roadmapRepository.findFirstByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(Optional.of(roadmapWithPhases(2)));
        when(sessionRepository.findByUserIdAndRoadmapIdOrderByNodeIndexAsc(userId, roadmapId)).thenReturn(List.of(
                session(0, SessionStatus.COMPLETED), session(1, SessionStatus.NOT_STARTED)));

        service.refillBuffer(userId);

        verify(generator, never()).generate(any(), any(), any(), anyInt(), any());
    }

    @Test
    void complete_setsStatusAndPublishesCompletedEvent() {
        LearningSession s = session(2, SessionStatus.IN_PROGRESS);
        when(sessionRepository.findByIdAndUserId(s.getId(), userId)).thenReturn(Optional.of(s));

        service.complete(userId, s.getId());

        assertThat(s.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        verify(events).publish(eq(KafkaTopics.LEARNING_SESSION_COMPLETED), eq(userId),
                any(LearningSessionCompletedEvent.class));
        verify(detail).evict(s.getId());
        verify(detail).getDetail(s.getId());
    }

    private OnboardingSession onboardingWithDomain(String domain) {
        OnboardingSession s = new OnboardingSession(userId, "Ada", "Lovelace");
        s.setDomain(domain);
        return s;
    }
}
