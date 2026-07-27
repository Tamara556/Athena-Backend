package com.athena.ai.dailyjourney.service.impl;

import com.athena.ai.constants.AiConstants;
import com.athena.ai.dailyjourney.domain.BlockStatus;
import com.athena.ai.dailyjourney.domain.BlockType;
import com.athena.ai.learningsession.domain.Difficulty;
import com.athena.ai.learningsession.domain.SessionStatus;
import com.athena.ai.learningsession.dto.LearningSessionResponse;
import com.athena.ai.dailyjourney.entity.DailyBlock;
import com.athena.ai.dailyjourney.entity.DailyMission;
import com.athena.ai.dailyjourney.entity.DailyReflection;
import com.athena.ai.roadmap.entity.GeneratedRoadmap;
import com.athena.ai.knowledgegraph.entity.KnowledgeNode;
import com.athena.ai.learningsession.entity.LearningSession;
import com.athena.ai.generation.messaging.AiEventPublisher;
import com.athena.ai.generation.model.DailyMissionPlan;
import com.athena.ai.generation.model.RoadmapContent;
import com.athena.ai.dailyjourney.repository.DailyBlockRepository;
import com.athena.ai.dailyjourney.repository.DailyMissionRepository;
import com.athena.ai.dailyjourney.repository.DailyReflectionRepository;
import com.athena.ai.roadmap.repository.GeneratedRoadmapRepository;
import com.athena.ai.learningsession.repository.LearningSessionRepository;
import com.athena.ai.onboarding.repository.OnboardingSessionRepository;
import com.athena.ai.generation.service.AiGenerationService;
import com.athena.ai.learningsession.service.LearningSessionDetailService;
import com.athena.common.event.DailyMissionGeneratedEvent;
import com.athena.common.event.KafkaTopics;
import com.athena.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyMissionGenerator {

    private final GeneratedRoadmapRepository roadmapRepository;
    private final LearningSessionRepository sessionRepository;
    private final OnboardingSessionRepository onboardingRepository;
    private final DailyMissionRepository missionRepository;
    private final DailyBlockRepository blockRepository;
    private final DailyReflectionRepository reflectionRepository;
    private final LearningSessionDetailService sessionDetail;
    private final AiGenerationService generation;
    private final DailyBlockComposer composer;
    private final WeakNodeFinder weakNodeFinder;
    private final AiEventPublisher events;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional
    public DailyMission generate(UUID userId, LocalDate missionDate) {
        GeneratedRoadmap roadmap = roadmapRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> ResourceNotFoundException.of(AiConstants.RESOURCE_ROADMAP, userId));
        LearningSession session = currentSession(userId);
        LearningSessionResponse detail = sessionDetail.getDetail(session.getId());
        List<KnowledgeNode> weakNodes = weakNodeFinder.find(userId);
        int availableMinutes = AiConstants.DEFAULT_AVAILABLE_MINUTES;

        DailyMissionPlan plan = generation.generateDailyMission(userId, roadmap.getGoal(), resolveDomain(userId),
                level(roadmap.getLevel()), session.getTitle(), objectives(roadmap, session.getNodeIndex()),
                weakAreasText(weakNodes), latestAdjustment(userId), availableMinutes);

        DailyMission mission = missionRepository.save(new DailyMission(userId, missionDate, roadmap.getId(),
                session.getRoadmapNodeId(), session.getId(), plan.mission().title(), plan.mission().description(),
                plan.mission().goalContext(), Difficulty.fromString(plan.mission().difficulty()), availableMinutes, 0));

        List<DailyBlock> blocks = composer.compose(mission, plan, detail, weakNodes, skipCounts(userId), availableMinutes);
        blockRepository.saveAll(blocks);
        mission.setEstimatedMinutes(composer.totalMinutes(blocks));
        missionRepository.save(mission);

        events.publish(KafkaTopics.DAILY_MISSION_GENERATED, userId,
                new DailyMissionGeneratedEvent(userId, mission.getId(), missionDate, Instant.now(clock)));
        log.info("Generated daily mission userId={} date={} missionId={} blocks={}",
                userId, missionDate, mission.getId(), blocks.size());
        return mission;
    }

    @Transactional
    public void regenerateRemaining(DailyMission mission) {
        GeneratedRoadmap roadmap = roadmapRepository.findById(mission.getRoadmapId())
                .orElseThrow(() -> ResourceNotFoundException.of(AiConstants.RESOURCE_ROADMAP, mission.getUserId()));
        LearningSession session = sessionRepository.findById(mission.getLearningSessionId())
                .orElseThrow(() -> ResourceNotFoundException.of(AiConstants.RESOURCE_LEARNING_SESSION, mission.getUserId()));

        List<DailyBlock> existing = blockRepository.findByMissionIdOrderByOrderIndexAsc(mission.getId());
        List<DailyBlock> kept = existing.stream().filter(this::isSettled).toList();
        existing.stream().filter(b -> !isSettled(b)).forEach(blockRepository::delete);

        int usedMinutes = kept.stream()
                .filter(b -> b.getStatus() == BlockStatus.COMPLETED)
                .mapToInt(DailyBlock::getDurationMinutes).sum();
        int remaining = Math.max(AiConstants.MIN_BLOCK_MINUTES, mission.getAvailableMinutes() - usedMinutes);

        List<KnowledgeNode> weakNodes = weakNodeFinder.find(mission.getUserId());
        DailyMissionPlan plan = generation.generateDailyMission(mission.getUserId(), roadmap.getGoal(),
                resolveDomain(mission.getUserId()), level(roadmap.getLevel()), session.getTitle(),
                objectives(roadmap, session.getNodeIndex()), weakAreasText(weakNodes),
                latestAdjustment(mission.getUserId()), remaining);

        List<DailyBlock> fresh = composer.compose(mission, plan, sessionDetail.getDetail(session.getId()),
                weakNodes, skipCounts(mission.getUserId()), remaining);
        for (int i = 0; i < fresh.size(); i++) {
            fresh.get(i).setOrderIndex(kept.size() + i);
        }
        blockRepository.saveAll(fresh);

        mission.setEstimatedMinutes(usedMinutes + composer.totalMinutes(fresh));
        missionRepository.save(mission);
    }

    private boolean isSettled(DailyBlock block) {
        return block.getStatus() == BlockStatus.COMPLETED || block.getStatus() == BlockStatus.SKIPPED;
    }

    private LearningSession currentSession(UUID userId) {
        return sessionRepository.findByUserIdOrderByNodeIndexAsc(userId).stream()
                .filter(s -> s.getStatus() != SessionStatus.COMPLETED)
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of(AiConstants.RESOURCE_LEARNING_SESSION, userId));
    }

    private Map<BlockType, Long> skipCounts(UUID userId) {
        Map<BlockType, Long> counts = new EnumMap<>(BlockType.class);
        for (BlockType type : BlockType.values()) {
            counts.put(type, blockRepository.countByUserIdAndTypeAndStatus(userId, type, BlockStatus.SKIPPED));
        }
        return counts;
    }

    private String latestAdjustment(UUID userId) {
        return reflectionRepository.findFirstByUserIdOrderByReflectionDateDesc(userId)
                .map(DailyReflection::getAdjustRequest)
                .filter(request -> request != null && !request.isBlank())
                .orElse("none");
    }

    private String weakAreasText(List<KnowledgeNode> weakNodes) {
        if (weakNodes.isEmpty()) {
            return "none";
        }
        return weakNodes.stream()
                .map(node -> node.getSkillName() + " (" + node.getMasteryPercentage() + "%)")
                .collect(Collectors.joining(", "));
    }

    private String objectives(GeneratedRoadmap roadmap, int nodeIndex) {
        RoadmapContent content = parse(roadmap);
        if (nodeIndex < 0 || nodeIndex >= content.phases().size()) {
            return "(none provided)";
        }
        List<String> objectives = content.phases().get(nodeIndex).objectives();
        if (objectives == null || objectives.isEmpty()) {
            return "(none provided)";
        }
        return objectives.stream().map(objective -> "- " + objective).collect(Collectors.joining("\n"));
    }

    private RoadmapContent parse(GeneratedRoadmap roadmap) {
        RoadmapContent content = objectMapper.readValue(roadmap.getContentJson(), RoadmapContent.class);
        return content.phases() == null ? new RoadmapContent(List.of()) : content;
    }

    private String resolveDomain(UUID userId) {
        return onboardingRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(session -> session.getDomain())
                .filter(domain -> domain != null && !domain.isBlank())
                .orElse("General");
    }

    private String level(String level) {
        return level == null || level.isBlank() ? "beginner" : level;
    }
}
