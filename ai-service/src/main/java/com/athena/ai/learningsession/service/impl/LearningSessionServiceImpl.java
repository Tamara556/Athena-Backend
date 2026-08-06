package com.athena.ai.learningsession.service.impl;

import com.athena.ai.llm.AiException;
import com.athena.ai.constants.AiConstants;
import com.athena.ai.learningsession.domain.SessionStatus;
import com.athena.ai.learningsession.dto.LearningSessionResponse;
import com.athena.ai.learningsession.dto.LearningSessionSummaryResponse;
import com.athena.ai.roadmap.entity.GeneratedRoadmap;
import com.athena.ai.learningsession.entity.LearningSession;
import com.athena.ai.generation.model.RoadmapContent;
import com.athena.ai.roadmap.repository.GeneratedRoadmapRepository;
import com.athena.ai.learningsession.repository.LearningSessionRepository;
import com.athena.ai.onboarding.repository.OnboardingSessionRepository;
import com.athena.ai.learningsession.service.LearningSessionDetailService;
import com.athena.ai.learningsession.service.LearningSessionService;
import com.athena.common.event.KafkaTopics;
import com.athena.common.event.LearningSessionCompletedEvent;
import com.athena.common.event.LearningSessionStartedEvent;
import com.athena.common.event.NodeBufferRefilledEvent;
import com.athena.common.event.RoadmapGeneratedEvent;
import com.athena.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningSessionServiceImpl implements LearningSessionService {

    private final LearningSessionRepository sessionRepository;
    private final GeneratedRoadmapRepository roadmapRepository;
    private final OnboardingSessionRepository onboardingRepository;
    private final LearningSessionGenerator generator;
    private final LearningSessionDetailService detail;
    private final com.athena.ai.generation.messaging.AiEventPublisher events;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public void generateInitialBuffer(UUID userId, UUID roadmapId) {
        GeneratedRoadmap roadmap = roadmapRepository.findById(roadmapId).orElse(null);
        if (roadmap == null) {
            log.warn("Cannot generate learning sessions; roadmap not found roadmapId={}", roadmapId);
            return;
        }
        RoadmapContent content = parse(roadmap);
        int nodes = content.phases().size();
        int count = Math.min(AiConstants.LEARNING_SESSION_BUFFER, nodes);
        String domain = resolveDomain(userId);
        for (int i = 0; i < count; i++) {
            tryGenerate(userId, roadmap, content, i, domain);
        }
        log.info("Seeded learning-session buffer userId={} roadmapId={} nodes={} buffer={}",
                userId, roadmapId, nodes, count);
    }

    @Override
    public void refillBuffer(UUID userId) {
        GeneratedRoadmap roadmap = roadmapRepository.findFirstByUserIdOrderByCreatedAtDesc(userId).orElse(null);
        if (roadmap == null) {
            return;
        }
        RoadmapContent content = parse(roadmap);
        int nodes = content.phases().size();

        Set<Integer> present = new HashSet<>();
        sessionRepository.findByUserIdAndRoadmapIdOrderByNodeIndexAsc(userId, roadmap.getId())
                .forEach(s -> present.add(s.getNodeIndex()));

        int nextIndex = -1;
        for (int i = 0; i < nodes; i++) {
            if (!present.contains(i)) {
                nextIndex = i;
                break;
            }
        }
        if (nextIndex < 0) {
            log.info("Learning-session buffer already complete userId={} roadmapId={}", userId, roadmap.getId());
            return;
        }

        LearningSession created = tryGenerate(userId, roadmap, content, nextIndex, resolveDomain(userId));
        if (created != null) {
            events.publish(KafkaTopics.NODE_BUFFER_REFILLED, userId,
                    new NodeBufferRefilledEvent(userId, roadmap.getId(), created.getId(), nextIndex,
                            Instant.now(clock)));
            log.info("Refilled learning-session buffer userId={} nodeIndex={}", userId, nextIndex);
        }
    }

    @Override
    public LearningSessionResponse prepareCurrent(UUID userId) {
        GeneratedRoadmap roadmap = roadmapRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> ResourceNotFoundException.of(AiConstants.RESOURCE_ROADMAP, userId));

        Optional<LearningSession> existing = sessionRepository.findByUserIdOrderByNodeIndexAsc(userId).stream()
                .filter(s -> s.getStatus() != SessionStatus.COMPLETED)
                .findFirst();
        if (existing.isPresent()) {
            return detail.getDetail(existing.get().getId());
        }

        RoadmapContent content = parse(roadmap);
        if (!content.phases().isEmpty()) {
            tryGenerate(userId, roadmap, content, 0, resolveDomain(userId));
            events.publish(KafkaTopics.ROADMAP_GENERATED, userId,
                    new RoadmapGeneratedEvent(userId, roadmap.getId(), Instant.now(clock)));
        }
        return getCurrent(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public LearningSessionResponse getCurrent(UUID userId) {
        LearningSession current = sessionRepository.findByUserIdOrderByNodeIndexAsc(userId).stream()
                .filter(s -> s.getStatus() != SessionStatus.COMPLETED)
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of(AiConstants.RESOURCE_LEARNING_SESSION, userId));
        return detail.getDetail(current.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public LearningSessionResponse getById(UUID userId, UUID sessionId) {
        LearningSession session = ownedSession(userId, sessionId);
        return detail.getDetail(session.getId());
    }

    @Override
    @Transactional
    public LearningSessionResponse start(UUID userId, UUID sessionId) {
        LearningSession session = ownedSession(userId, sessionId);
        if (session.getStatus() == SessionStatus.NOT_STARTED) {
            session.setStatus(SessionStatus.IN_PROGRESS);
            sessionRepository.save(session);
            events.publish(KafkaTopics.LEARNING_SESSION_STARTED, userId,
                    new LearningSessionStartedEvent(userId, session.getId(), Instant.now(clock)));
        }
        detail.evict(session.getId());
        return detail.getDetail(session.getId());
    }

    @Override
    @Transactional
    public LearningSessionResponse complete(UUID userId, UUID sessionId) {
        LearningSession session = ownedSession(userId, sessionId);
        if (session.getStatus() != SessionStatus.COMPLETED) {
            session.setStatus(SessionStatus.COMPLETED);
            sessionRepository.save(session);
            events.publish(KafkaTopics.LEARNING_SESSION_COMPLETED, userId,
                    new LearningSessionCompletedEvent(userId, session.getId(), session.getNodeIndex(),
                            Instant.now(clock)));
        }
        detail.evict(session.getId());
        return detail.getDetail(session.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearningSessionSummaryResponse> getUpcoming(UUID userId) {
        return sessionRepository.findByUserIdOrderByNodeIndexAsc(userId).stream()
                .filter(s -> s.getStatus() != SessionStatus.COMPLETED)
                .map(s -> new LearningSessionSummaryResponse(s.getId(), s.getRoadmapNodeId(), s.getNodeIndex(),
                        s.getTitle(), s.getStatus().name(), s.getEstimatedMinutes()))
                .toList();
    }

    private LearningSession tryGenerate(UUID userId, GeneratedRoadmap roadmap, RoadmapContent content,
                                        int nodeIndex, String domain) {
        try {
            return generator.generate(userId, roadmap, content, nodeIndex, domain);
        } catch (AiException ex) {
            log.warn("Learning-session generation failed userId={} nodeIndex={} cause={}; will retry on next trigger",
                    userId, nodeIndex, ex.getClass().getSimpleName());
            return null;
        } catch (org.springframework.dao.DataAccessException ex) {
            log.info("Learning-session node already generated concurrently userId={} nodeIndex={}", userId, nodeIndex);
            return null;
        }
    }

    private LearningSession ownedSession(UUID userId, UUID sessionId) {
        return sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of(AiConstants.RESOURCE_LEARNING_SESSION, sessionId));
    }

    private RoadmapContent parse(GeneratedRoadmap roadmap) {
        RoadmapContent content = objectMapper.readValue(roadmap.getContentJson(), RoadmapContent.class);
        if (content.phases() == null) {
            return new RoadmapContent(List.of());
        }
        return content;
    }

    private String resolveDomain(UUID userId) {
        return onboardingRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(s -> s.getDomain())
                .filter(d -> d != null && !d.isBlank())
                .orElse("General");
    }
}
