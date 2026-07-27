package com.athena.ai.dailyjourney.service.impl;

import com.athena.ai.constants.AiConstants;
import com.athena.ai.dailyjourney.domain.AdjustmentType;
import com.athena.ai.dailyjourney.domain.BlockStatus;
import com.athena.ai.dailyjourney.domain.BlockType;
import com.athena.ai.dailyjourney.domain.ConfidenceLevel;
import com.athena.ai.learningsession.domain.Difficulty;
import com.athena.ai.learningsession.domain.PracticeType;
import com.athena.ai.dailyjourney.entity.DailyBlock;
import com.athena.ai.dailyjourney.entity.DailyCheckin;
import com.athena.ai.dailyjourney.entity.DailyMission;
import com.athena.ai.knowledgegraph.entity.KnowledgeNode;
import com.athena.ai.learningsession.entity.PracticeActivity;
import com.athena.ai.generation.messaging.AiEventPublisher;
import com.athena.ai.generation.model.DrillContent;
import com.athena.ai.dailyjourney.repository.AdjustmentLogRepository;
import com.athena.ai.dailyjourney.repository.DailyBlockRepository;
import com.athena.ai.dailyjourney.repository.DailyCheckinRepository;
import com.athena.ai.knowledgegraph.repository.KnowledgeNodeRepository;
import com.athena.ai.learningsession.repository.PracticeActivityRepository;
import com.athena.ai.generation.service.AiGenerationService;
import com.athena.ai.dailyjourney.entity.AdjustmentLog;
import com.athena.common.event.DailyMissionAdjustedEvent;
import com.athena.common.event.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdjustmentEngine {

    private static final double SIMPLIFY_DURATION_FACTOR = 0.85;
    private static final String GENERAL_DOMAIN = "General";
    private static final int NEUTRAL_MASTERY = 50;
    private static final int CHALLENGE_MASTERY = 80;

    private final DailyBlockRepository blockRepository;
    private final AdjustmentLogRepository adjustmentRepository;
    private final DailyCheckinRepository checkinRepository;
    private final KnowledgeNodeRepository knowledgeRepository;
    private final PracticeActivityRepository practiceRepository;
    private final AiGenerationService generation;
    private final AiEventPublisher events;
    private final Clock clock;

    public void record(DailyMission mission, AdjustmentType type, String reason, UUID affectedBlockId) {
        adjustmentRepository.save(new AdjustmentLog(mission.getId(), mission.getUserId(), type, reason, affectedBlockId));
        events.publish(KafkaTopics.DAILY_MISSION_ADJUSTED, mission.getUserId(),
                new DailyMissionAdjustedEvent(mission.getUserId(), mission.getId(), type.name(), reason,
                        affectedBlockId, Instant.now(clock)));
    }

    public void afterBlockCompleted(DailyMission mission, DailyBlock block) {
        ConfidenceLevel confidence = latestConfidence(mission.getId());
        if (completedFast(block) && confidence == ConfidenceLevel.CONFIDENT) {
            insertChallenge(mission, block);
        }
        if (block.getType() == BlockType.QUIZ && block.getProgressPercent() < AiConstants.LOW_CONFIDENCE_PERCENT) {
            insertReview(mission, block);
        }
    }

    public void afterCheckin(DailyMission mission, ConfidenceLevel confidence, DailyBlock contextBlock) {
        if (confidence == ConfidenceLevel.NEED_HELP) {
            simplify(mission);
        } else if (confidence == ConfidenceLevel.UNSURE && contextBlock != null) {
            insertReview(mission, contextBlock);
        }
    }

    public void simplify(DailyMission mission) {
        adjustUpcoming(mission, Difficulty::easier, SIMPLIFY_DURATION_FACTOR);
        record(mission, AdjustmentType.SIMPLIFY, "Eased the difficulty of what's coming up", null);
    }

    public void intensify(DailyMission mission) {
        adjustUpcoming(mission, Difficulty::harder, 1.0);
        record(mission, AdjustmentType.ADD, "Raised the challenge of what's coming up", null);
    }

    public void insertStrengthenDrill(DailyMission mission, KnowledgeNode node) {
        String domain = node.getDomain() != null ? node.getDomain() : GENERAL_DOMAIN;
        insertDrill(mission, node.getSkillName(), domain, node.getMasteryPercentage(), node.getId(), BlockType.DRILL,
                Difficulty.MODERATE, AdjustmentType.ADD, "Added a drill to strengthen " + node.getSkillName());
    }

    private void insertChallenge(DailyMission mission, DailyBlock block) {
        insertDrill(mission, block.getTitle(), GENERAL_DOMAIN, CHALLENGE_MASTERY, null, BlockType.DRILL,
                Difficulty.CHALLENGING, AdjustmentType.ADD, "Added an advanced challenge");
    }

    private void insertReview(DailyMission mission, DailyBlock block) {
        KnowledgeNode node = block.getKnowledgeNodeId() == null
                ? null
                : knowledgeRepository.findById(block.getKnowledgeNodeId()).orElse(null);
        String skill = node != null ? node.getSkillName() : block.getTitle();
        String domain = node != null && node.getDomain() != null ? node.getDomain() : GENERAL_DOMAIN;
        int mastery = node != null ? node.getMasteryPercentage() : NEUTRAL_MASTERY;
        insertDrill(mission, skill, domain, mastery, block.getKnowledgeNodeId(), BlockType.REVIEW,
                block.getDifficulty().easier(), AdjustmentType.REVIEW, "Slipped in a short review of " + skill);
    }

    private void insertDrill(DailyMission mission, String skill, String domain, int mastery, UUID knowledgeNodeId,
                             BlockType type, Difficulty difficulty, AdjustmentType adjustment, String reason) {
        DrillContent drill = generation.generateWeaknessDrill(mission.getUserId(), skill, domain, mastery);
        UUID sourceRef = persistDrillActivity(mission, drill);
        int nextIndex = blockRepository.findByMissionIdOrderByOrderIndexAsc(mission.getId()).size();
        DailyBlock created = blockRepository.save(new DailyBlock(mission.getId(), mission.getUserId(), nextIndex, type,
                drill.title(), drill.description(), mission.getRoadmapNodeId(), knowledgeNodeId, sourceRef, difficulty,
                drill.durationMinutes(), BlockStatus.UPCOMING, true));
        record(mission, adjustment, reason, created.getId());
    }

    private UUID persistDrillActivity(DailyMission mission, DrillContent drill) {
        UUID sessionId = mission.getLearningSessionId();
        if (sessionId == null) {
            return null;
        }
        int order = practiceRepository.findBySessionIdOrderByOrderIndexAsc(sessionId).size();
        PracticeActivity activity = practiceRepository.save(new PracticeActivity(sessionId, drill.title(),
                drill.description(), PracticeType.REFLECTION, drill.instructions(), null, drill.durationMinutes(), order));
        return activity.getId();
    }

    private void adjustUpcoming(DailyMission mission, java.util.function.UnaryOperator<Difficulty> shift,
                               double durationFactor) {
        blockRepository.findByMissionIdOrderByOrderIndexAsc(mission.getId()).stream()
                .filter(b -> b.getStatus() == BlockStatus.UPCOMING || b.getStatus() == BlockStatus.CURRENT)
                .forEach(b -> {
                    b.setDifficulty(shift.apply(b.getDifficulty()));
                    b.setDurationMinutes(Math.max(AiConstants.MIN_BLOCK_MINUTES,
                            (int) Math.round(b.getDurationMinutes() * durationFactor)));
                    blockRepository.save(b);
                });
    }

    private boolean completedFast(DailyBlock block) {
        if (block.getStartedAt() == null || block.getCompletedAt() == null) {
            return false;
        }
        long actualSeconds = Duration.between(block.getStartedAt(), block.getCompletedAt()).getSeconds();
        long budgetSeconds = (long) (block.getDurationMinutes() * 60 * AiConstants.FAST_COMPLETION_RATIO);
        return actualSeconds < budgetSeconds;
    }

    private ConfidenceLevel latestConfidence(UUID missionId) {
        return checkinRepository.findFirstByMissionIdOrderByCreatedAtDesc(missionId)
                .map(DailyCheckin::getConfidence)
                .orElse(null);
    }
}
