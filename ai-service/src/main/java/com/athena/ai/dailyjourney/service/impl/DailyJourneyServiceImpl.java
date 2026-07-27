package com.athena.ai.dailyjourney.service.impl;

import com.athena.ai.constants.AiConstants;
import com.athena.ai.dailyjourney.domain.AdjustAction;
import com.athena.ai.dailyjourney.domain.AdjustmentType;
import com.athena.ai.dailyjourney.domain.BlockStatus;
import com.athena.ai.dailyjourney.domain.ConfidenceLevel;
import com.athena.ai.dailyjourney.domain.DayStatus;
import com.athena.ai.dailyjourney.dto.DailyJourneyResponse;
import com.athena.ai.dailyjourney.entity.DailyBlock;
import com.athena.ai.dailyjourney.entity.DailyCheckin;
import com.athena.ai.dailyjourney.entity.DailyMission;
import com.athena.ai.dailyjourney.entity.DailyReflection;
import com.athena.ai.roadmap.entity.GeneratedRoadmap;
import com.athena.ai.knowledgegraph.entity.KnowledgeNode;
import com.athena.ai.generation.messaging.AiEventPublisher;
import com.athena.ai.generation.model.MentorReply;
import com.athena.ai.generation.model.WhyReasoning;
import com.athena.ai.dailyjourney.repository.DailyBlockRepository;
import com.athena.ai.dailyjourney.repository.DailyCheckinRepository;
import com.athena.ai.dailyjourney.repository.DailyMissionRepository;
import com.athena.ai.dailyjourney.repository.DailyReflectionRepository;
import com.athena.ai.roadmap.repository.GeneratedRoadmapRepository;
import com.athena.ai.knowledgegraph.repository.KnowledgeNodeRepository;
import com.athena.ai.generation.service.AiGenerationService;
import com.athena.ai.dailyjourney.service.DailyJourneyDetailService;
import com.athena.ai.dailyjourney.service.DailyJourneyService;
import com.athena.common.event.DailyBlockCompletedEvent;
import com.athena.common.event.DailyBlockSkippedEvent;
import com.athena.common.event.DailyCheckinRecordedEvent;
import com.athena.common.event.DailyReflectionSavedEvent;
import com.athena.common.event.KafkaTopics;
import com.athena.common.event.TaskCompletedEvent;
import com.athena.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyJourneyServiceImpl implements DailyJourneyService {

    private static final String TASK_TYPE_DAILY_MISSION = "DAILY_MISSION";
    private static final String RESOURCE_KNOWLEDGE_NODE = "Knowledge node";

    private final DailyMissionRepository missionRepository;
    private final DailyBlockRepository blockRepository;
    private final DailyCheckinRepository checkinRepository;
    private final DailyReflectionRepository reflectionRepository;
    private final KnowledgeNodeRepository knowledgeRepository;
    private final GeneratedRoadmapRepository roadmapRepository;
    private final DailyMissionGenerator generator;
    private final DailyJourneyDetailService detail;
    private final AdjustmentEngine adjustmentEngine;
    private final AiGenerationService generation;
    private final WeakNodeFinder weakNodeFinder;
    private final AiEventPublisher events;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public DailyJourneyResponse getToday(UUID userId) {
        DailyMission mission = missionRepository.findByUserIdAndMissionDate(userId, today())
                .orElseGet(() -> generateOrReread(userId));
        return detail.getDetail(mission.getId());
    }

    @Override
    @Transactional
    public DailyJourneyResponse startDay(UUID userId) {
        DailyMission mission = requireToday(userId);
        if (mission.getStatus() == DayStatus.READY || mission.getStatus() == DayStatus.FORMING) {
            mission.setStatus(DayStatus.IN_PROGRESS);
            mission.setStartedAt(Instant.now(clock));
            missionRepository.save(mission);
        }
        return refreshed(mission.getId());
    }

    @Override
    @Transactional
    public WhyReasoning getWhy(UUID userId) {
        DailyMission mission = requireToday(userId);
        if (mission.getReasoningJson() != null) {
            return objectMapper.readValue(mission.getReasoningJson(), WhyReasoning.class);
        }
        WhyReasoning reasoning = generateWhy(userId, mission);
        mission.setReasoningJson(objectMapper.writeValueAsString(reasoning));
        missionRepository.save(mission);
        return reasoning;
    }

    @Override
    @Transactional
    public DailyJourneyResponse adjustPlan(UUID userId, AdjustAction action) {
        DailyMission mission = requireToday(userId);
        switch (action) {
            case SIMPLIFY -> adjustmentEngine.simplify(mission);
            case INTENSIFY -> adjustmentEngine.intensify(mission);
            case REGENERATE -> {
                generator.regenerateRemaining(mission);
                adjustmentEngine.record(mission, AdjustmentType.REGENERATE, "Rebuilt the rest of today's plan", null);
            }
        }
        return refreshed(mission.getId());
    }

    @Override
    @Transactional
    public DailyJourneyResponse adjustTime(UUID userId, int availableMinutes) {
        DailyMission mission = requireToday(userId);
        mission.setAvailableMinutes(availableMinutes);
        List<DailyBlock> blocks = blockRepository.findByMissionIdOrderByOrderIndexAsc(mission.getId());
        int settledMinutes = sumMinutes(blocks, BlockStatus.COMPLETED);
        List<DailyBlock> upcoming = blocks.stream().filter(this::isOpen).toList();
        int upcomingMinutes = upcoming.stream().mapToInt(DailyBlock::getDurationMinutes).sum();
        int remaining = Math.max(0, availableMinutes - settledMinutes);

        if (upcomingMinutes > remaining && upcomingMinutes > 0) {
            double scale = (double) remaining / upcomingMinutes;
            upcoming.forEach(block -> {
                block.setDurationMinutes(Math.max(AiConstants.MIN_BLOCK_MINUTES,
                        (int) Math.round(block.getDurationMinutes() * scale)));
                blockRepository.save(block);
            });
            adjustmentEngine.record(mission, AdjustmentType.TRIM,
                    "Trimmed the plan to fit " + availableMinutes + " min", null);
        }
        mission.setEstimatedMinutes(estimatedMinutes(mission.getId()));
        missionRepository.save(mission);
        return refreshed(mission.getId());
    }

    @Override
    @Transactional
    public DailyJourneyResponse startBlock(UUID userId, UUID blockId) {
        DailyBlock block = ownedBlock(userId, blockId);
        if (block.getStatus() == BlockStatus.UPCOMING || block.getStatus() == BlockStatus.CURRENT) {
            block.setStatus(BlockStatus.CURRENT);
            if (block.getStartedAt() == null) {
                block.setStartedAt(Instant.now(clock));
            }
            blockRepository.save(block);
        }
        return refreshed(block.getMissionId());
    }

    @Override
    @Transactional
    public DailyJourneyResponse updateProgress(UUID userId, UUID blockId, int percent) {
        DailyBlock block = ownedBlock(userId, blockId);
        block.setProgressPercent(Math.max(0, Math.min(100, percent)));
        blockRepository.save(block);
        return refreshed(block.getMissionId());
    }

    @Override
    @Transactional
    public DailyJourneyResponse completeBlock(UUID userId, UUID blockId) {
        DailyBlock block = ownedBlock(userId, blockId);
        if (block.getStatus() != BlockStatus.COMPLETED) {
            block.setStatus(BlockStatus.COMPLETED);
            block.setProgressPercent(100);
            block.setCompletedAt(Instant.now(clock));
            blockRepository.save(block);
            events.publish(KafkaTopics.DAILY_BLOCK_COMPLETED, userId,
                    new DailyBlockCompletedEvent(userId, block.getMissionId(), block.getId(), block.getType().name(),
                            block.getDurationMinutes(), block.getKnowledgeNodeId(), Instant.now(clock)));
            completeMissionIfDone(userId, block.getMissionId());
        }
        return refreshed(block.getMissionId());
    }

    @Override
    @Transactional
    public DailyJourneyResponse skipBlock(UUID userId, UUID blockId, String reason) {
        DailyBlock block = ownedBlock(userId, blockId);
        block.setStatus(BlockStatus.SKIPPED);
        block.setSkipReason(reason);
        blockRepository.save(block);
        events.publish(KafkaTopics.DAILY_BLOCK_SKIPPED, userId,
                new DailyBlockSkippedEvent(userId, block.getMissionId(), block.getId(), block.getType().name(),
                        reason, Instant.now(clock)));
        return refreshed(block.getMissionId());
    }

    @Override
    @Transactional
    public DailyJourneyResponse relinkBlock(UUID userId, UUID blockId) {
        DailyBlock block = ownedBlock(userId, blockId);
        block.setStatus(BlockStatus.UPCOMING);
        block.setSkipReason(null);
        blockRepository.save(block);
        missionRepository.findById(block.getMissionId()).ifPresent(mission -> {
            mission.setEstimatedMinutes(estimatedMinutes(mission.getId()));
            missionRepository.save(mission);
        });
        return refreshed(block.getMissionId());
    }

    @Override
    @Transactional
    public DailyJourneyResponse strengthen(UUID userId, UUID knowledgeNodeId) {
        DailyMission mission = requireToday(userId);
        KnowledgeNode node = knowledgeRepository.findById(knowledgeNodeId)
                .filter(candidate -> candidate.getUserId().equals(userId))
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE_KNOWLEDGE_NODE, knowledgeNodeId));
        adjustmentEngine.insertStrengthenDrill(mission, node);
        return refreshed(mission.getId());
    }

    @Override
    @Transactional
    public DailyJourneyResponse checkin(UUID userId, ConfidenceLevel confidence, UUID blockId) {
        DailyMission mission = requireToday(userId);
        DailyBlock block = blockId == null ? null : blockRepository.findByIdAndUserId(blockId, userId).orElse(null);
        String topic = block != null ? block.getTitle() : mission.getTitle();
        MentorReply reply = generation.generateMentorReply(userId, topic, confidence.name());
        checkinRepository.save(new DailyCheckin(mission.getId(), userId, blockId, confidence, topic, reply.reply()));
        events.publish(KafkaTopics.DAILY_CHECKIN_RECORDED, userId,
                new DailyCheckinRecordedEvent(userId, mission.getId(), confidence.name(), Instant.now(clock)));
        return refreshed(mission.getId());
    }

    @Override
    @Transactional
    public DailyJourneyResponse saveReflection(UUID userId, String hardestPart, String whatClicked,
                                               String adjustRequest) {
        DailyMission mission = requireToday(userId);
        DailyReflection reflection = reflectionRepository.findByMissionId(mission.getId())
                .orElseGet(() -> new DailyReflection(mission.getId(), userId, today(), null, null, null, false));
        reflection.setHardestPart(hardestPart);
        reflection.setWhatClicked(whatClicked);
        reflection.setAdjustRequest(adjustRequest);
        reflection.setSkipped(false);
        reflectionRepository.save(reflection);
        markReflected(mission);
        events.publish(KafkaTopics.DAILY_REFLECTION_SAVED, userId,
                new DailyReflectionSavedEvent(userId, mission.getId(), today(), false, Instant.now(clock)));
        return refreshed(mission.getId());
    }

    @Override
    @Transactional
    public DailyJourneyResponse skipReflection(UUID userId) {
        DailyMission mission = requireToday(userId);
        DailyReflection reflection = reflectionRepository.findByMissionId(mission.getId())
                .orElseGet(() -> new DailyReflection(mission.getId(), userId, today(), null, null, null, true));
        reflection.setSkipped(true);
        reflectionRepository.save(reflection);
        markReflected(mission);
        events.publish(KafkaTopics.DAILY_REFLECTION_SAVED, userId,
                new DailyReflectionSavedEvent(userId, mission.getId(), today(), true, Instant.now(clock)));
        return refreshed(mission.getId());
    }

    private void completeMissionIfDone(UUID userId, UUID missionId) {
        List<DailyBlock> blocks = blockRepository.findByMissionIdOrderByOrderIndexAsc(missionId);
        boolean allSettled = blocks.stream().allMatch(b -> b.getStatus() == BlockStatus.COMPLETED
                || b.getStatus() == BlockStatus.SKIPPED);
        boolean anyCompleted = blocks.stream().anyMatch(b -> b.getStatus() == BlockStatus.COMPLETED);
        if (!allSettled || !anyCompleted) {
            return;
        }
        DailyMission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> ResourceNotFoundException.of(AiConstants.RESOURCE_DAILY_MISSION, missionId));
        if (mission.getStatus() == DayStatus.COMPLETED || mission.getStatus() == DayStatus.REFLECTED) {
            return;
        }
        mission.setStatus(DayStatus.COMPLETED);
        mission.setCompletedAt(Instant.now(clock));
        missionRepository.save(mission);
        events.publish(KafkaTopics.TASK_COMPLETED, userId,
                new TaskCompletedEvent(userId, mission.getId(), mission.getId(), TASK_TYPE_DAILY_MISSION,
                        sumMinutes(blocks, BlockStatus.COMPLETED), Instant.now(clock)));
    }

    private void markReflected(DailyMission mission) {
        mission.setStatus(DayStatus.REFLECTED);
        missionRepository.save(mission);
    }

    private WhyReasoning generateWhy(UUID userId, DailyMission mission) {
        GeneratedRoadmap roadmap = roadmapRepository.findById(mission.getRoadmapId()).orElse(null);
        List<KnowledgeNode> all = knowledgeRepository.findByUserIdOrderBySkillNameAsc(userId);
        String mastered = all.stream()
                .max(Comparator.comparingInt(KnowledgeNode::getMasteryPercentage))
                .map(KnowledgeNode::getSkillName)
                .orElse("your fundamentals");
        List<KnowledgeNode> weak = weakNodeFinder.find(userId);
        String weakAreas = weak.isEmpty() ? "none"
                : weak.stream().map(KnowledgeNode::getSkillName).collect(Collectors.joining(", "));
        String interview = weak.isEmpty() ? "your recent learning activity"
                : "a recent assessment surfaced a gap in " + weak.get(0).getSkillName();
        String goal = roadmap != null ? roadmap.getGoal() : "your goal";
        return generation.generateWhyReasoning(userId, goal, mission.getTitle(), interview, mastered, weakAreas);
    }

    private DailyMission generateOrReread(UUID userId) {
        try {
            return generator.generate(userId, today());
        } catch (DataAccessException ex) {
            return missionRepository.findByUserIdAndMissionDate(userId, today())
                    .orElseThrow(() -> ResourceNotFoundException.of(AiConstants.RESOURCE_DAILY_MISSION, userId));
        }
    }

    private DailyMission requireToday(UUID userId) {
        return missionRepository.findByUserIdAndMissionDate(userId, today())
                .orElseThrow(() -> ResourceNotFoundException.of(AiConstants.RESOURCE_DAILY_MISSION, userId));
    }

    private DailyBlock ownedBlock(UUID userId, UUID blockId) {
        return blockRepository.findByIdAndUserId(blockId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of(AiConstants.RESOURCE_DAILY_BLOCK, blockId));
    }

    private DailyJourneyResponse refreshed(UUID missionId) {
        detail.evict(missionId);
        return detail.getDetail(missionId);
    }

    private boolean isOpen(DailyBlock block) {
        return block.getStatus() == BlockStatus.UPCOMING || block.getStatus() == BlockStatus.CURRENT;
    }

    private int sumMinutes(List<DailyBlock> blocks, BlockStatus status) {
        return blocks.stream().filter(b -> b.getStatus() == status).mapToInt(DailyBlock::getDurationMinutes).sum();
    }

    private int estimatedMinutes(UUID missionId) {
        return blockRepository.findByMissionIdOrderByOrderIndexAsc(missionId).stream()
                .filter(b -> b.getStatus() != BlockStatus.SKIPPED)
                .mapToInt(DailyBlock::getDurationMinutes).sum();
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
