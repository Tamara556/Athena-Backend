package com.athena.ai.dailyjourney.service.impl;

import com.athena.ai.constants.AiConstants;
import com.athena.ai.dailyjourney.domain.BlockStatus;
import com.athena.ai.dailyjourney.domain.BlockType;
import com.athena.ai.learningsession.domain.Difficulty;
import com.athena.ai.learningsession.dto.LearningSessionResponse;
import com.athena.ai.dailyjourney.entity.DailyBlock;
import com.athena.ai.dailyjourney.entity.DailyMission;
import com.athena.ai.knowledgegraph.entity.KnowledgeNode;
import com.athena.ai.generation.model.DailyMissionPlan;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class DailyBlockComposer {

    private static final double SKIP_DURATION_FACTOR = 0.7;

    public List<DailyBlock> compose(DailyMission mission, DailyMissionPlan plan, LearningSessionResponse session,
                                    List<KnowledgeNode> weakNodes, Map<BlockType, Long> skipCounts,
                                    int availableMinutes) {
        List<DailyMissionPlan.Block> planBlocks = plan == null || plan.blocks() == null ? List.of() : plan.blocks();
        Map<BlockType, Deque<UUID>> sources = sourceRefs(session);
        Deque<KnowledgeNode> weak = new ArrayDeque<>(weakNodes);
        double scale = budgetScale(planBlocks, availableMinutes);

        List<DailyBlock> blocks = new ArrayList<>(planBlocks.size());
        for (int i = 0; i < planBlocks.size(); i++) {
            DailyMissionPlan.Block source = planBlocks.get(i);
            BlockType type = BlockType.fromString(source.type());
            Difficulty difficulty = Difficulty.min(positionDifficulty(i, planBlocks.size()), mission.getDifficulty());
            int duration = Math.max(AiConstants.MIN_BLOCK_MINUTES, (int) Math.round(source.durationMinutes() * scale));

            if (skipCounts.getOrDefault(type, 0L) >= AiConstants.SKIP_ADAPT_THRESHOLD) {
                duration = Math.max(AiConstants.MIN_BLOCK_MINUTES, (int) Math.round(duration * SKIP_DURATION_FACTOR));
                difficulty = difficulty.easier();
            }

            UUID sourceRef = poll(sources.get(type));
            UUID knowledgeNodeId = isWeaknessBlock(type) ? pollNodeId(weak) : null;
            BlockStatus status = i == 0 ? BlockStatus.CURRENT : BlockStatus.UPCOMING;

            blocks.add(new DailyBlock(mission.getId(), mission.getUserId(), i, type, source.title(),
                    source.description(), mission.getRoadmapNodeId(), knowledgeNodeId, sourceRef, difficulty,
                    duration, status, false));
        }
        return blocks;
    }

    public int totalMinutes(List<DailyBlock> blocks) {
        return blocks.stream().mapToInt(DailyBlock::getDurationMinutes).sum();
    }

    private Map<BlockType, Deque<UUID>> sourceRefs(LearningSessionResponse session) {
        Map<BlockType, Deque<UUID>> sources = new EnumMap<>(BlockType.class);
        if (session == null) {
            return sources;
        }
        sources.put(BlockType.READING, session.readings().stream()
                .map(r -> r.id()).collect(Collectors.toCollection(ArrayDeque::new)));
        sources.put(BlockType.VIDEO, session.watchings().stream()
                .map(w -> w.id()).collect(Collectors.toCollection(ArrayDeque::new)));
        sources.put(BlockType.PRACTICE, session.practices().stream()
                .map(p -> p.id()).collect(Collectors.toCollection(ArrayDeque::new)));
        sources.put(BlockType.QUIZ, session.quizzes().stream()
                .map(q -> q.id()).collect(Collectors.toCollection(ArrayDeque::new)));
        return sources;
    }

    private double budgetScale(List<DailyMissionPlan.Block> blocks, int availableMinutes) {
        int planTotal = blocks.stream().mapToInt(DailyMissionPlan.Block::durationMinutes).sum();
        return planTotal > availableMinutes && planTotal > 0 ? (double) availableMinutes / planTotal : 1.0;
    }

    private Difficulty positionDifficulty(int index, int size) {
        if (index == 0) {
            return Difficulty.EASY;
        }
        return index == size - 1 ? Difficulty.CHALLENGING : Difficulty.MODERATE;
    }

    private boolean isWeaknessBlock(BlockType type) {
        return type == BlockType.REVIEW || type == BlockType.DRILL;
    }

    private UUID poll(Deque<UUID> queue) {
        return queue == null || queue.isEmpty() ? null : queue.poll();
    }

    private UUID pollNodeId(Deque<KnowledgeNode> queue) {
        return queue.isEmpty() ? null : queue.poll().getId();
    }
}
