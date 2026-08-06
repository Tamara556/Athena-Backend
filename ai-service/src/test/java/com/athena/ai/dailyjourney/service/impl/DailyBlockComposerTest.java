package com.athena.ai.dailyjourney.service.impl;

import com.athena.ai.dailyjourney.domain.BlockStatus;
import com.athena.ai.dailyjourney.domain.BlockType;
import com.athena.ai.dailyjourney.entity.DailyBlock;
import com.athena.ai.dailyjourney.entity.DailyMission;
import com.athena.ai.generation.model.DailyMissionPlan;
import com.athena.ai.knowledgegraph.entity.KnowledgeNode;
import com.athena.ai.learningsession.domain.Difficulty;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DailyBlockComposerTest {

    private final DailyBlockComposer composer = new DailyBlockComposer();
    private final UUID userId = UUID.randomUUID();

    @Test
    void assignsPositionDifficultyAndCurrentStatusToFirstBlock() {
        List<DailyBlock> blocks = composer.compose(mission(Difficulty.CHALLENGING),
                plan(block("READING", 20), block("READING", 20), block("READING", 20)),
                null, List.of(), Map.of(), 1000);

        assertThat(blocks).extracting(DailyBlock::getDifficulty)
                .containsExactly(Difficulty.EASY, Difficulty.MODERATE, Difficulty.CHALLENGING);
        assertThat(blocks).extracting(DailyBlock::getStatus)
                .containsExactly(BlockStatus.CURRENT, BlockStatus.UPCOMING, BlockStatus.UPCOMING);
        assertThat(blocks).extracting(DailyBlock::getOrderIndex).containsExactly(0, 1, 2);
    }

    @Test
    void scalesDurationsDownToFitAvailableMinutes() {
        List<DailyBlock> blocks = composer.compose(mission(Difficulty.CHALLENGING),
                plan(block("READING", 60), block("READING", 60)),
                null, List.of(), Map.of(), 60);

        // plan total 120 > 60 available -> scale 0.5.
        assertThat(blocks).extracting(DailyBlock::getDurationMinutes).containsExactly(30, 30);
        assertThat(composer.totalMinutes(blocks)).isEqualTo(60);
    }

    @Test
    void neverRaisesDifficultyAboveTheMissionCeiling() {
        List<DailyBlock> blocks = composer.compose(mission(Difficulty.EASY),
                plan(block("READING", 20), block("READING", 20), block("READING", 20)),
                null, List.of(), Map.of(), 1000);

        assertThat(blocks).extracting(DailyBlock::getDifficulty)
                .containsOnly(Difficulty.EASY);
    }

    @Test
    void easesDurationAndDifficultyForFrequentlySkippedBlockTypes() {
        List<DailyBlock> blocks = composer.compose(mission(Difficulty.CHALLENGING),
                plan(block("READING", 20), block("READING", 20)),
                null, List.of(), Map.of(BlockType.READING, 3L), 1000);

        // Last block would be CHALLENGING, but the skip streak eases it and shrinks duration (x0.7).
        assertThat(blocks.get(1).getDifficulty()).isEqualTo(Difficulty.MODERATE);
        assertThat(blocks).extracting(DailyBlock::getDurationMinutes).containsExactly(14, 14);
    }

    @Test
    void enforcesMinimumBlockDuration() {
        List<DailyBlock> blocks = composer.compose(mission(Difficulty.CHALLENGING),
                plan(block("READING", 2)), null, List.of(), Map.of(), 1000);

        assertThat(blocks.getFirst().getDurationMinutes()).isEqualTo(5);
    }

    @Test
    void routesKnowledgeNodesOnlyToWeaknessBlocks() {
        KnowledgeNode weak = new KnowledgeNode(userId, "Joins", "db", 30, 0.3);
        UUID nodeId = UUID.randomUUID();
        weak.setId(nodeId);

        List<DailyBlock> blocks = composer.compose(mission(Difficulty.CHALLENGING),
                plan(block("DRILL", 20), block("READING", 20)),
                null, List.of(weak), Map.of(), 1000);

        assertThat(blocks.get(0).getType()).isEqualTo(BlockType.DRILL);
        assertThat(blocks.get(0).getKnowledgeNodeId()).isEqualTo(nodeId);
        assertThat(blocks.get(1).getKnowledgeNodeId()).isNull();
    }

    @Test
    void returnsEmptyForNullOrEmptyPlan() {
        assertThat(composer.compose(mission(Difficulty.MODERATE), null, null, List.of(), Map.of(), 100)).isEmpty();
        assertThat(composer.compose(mission(Difficulty.MODERATE), new DailyMissionPlan(null, null),
                null, List.of(), Map.of(), 100)).isEmpty();
    }

    private DailyMission mission(Difficulty difficulty) {
        DailyMission mission = new DailyMission(userId, LocalDate.now(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "title", "desc", "ctx", difficulty, 105, 100);
        mission.setId(UUID.randomUUID());
        return mission;
    }

    private DailyMissionPlan plan(DailyMissionPlan.Block... blocks) {
        return new DailyMissionPlan(
                new DailyMissionPlan.Mission("t", "d", "g", "MODERATE"), List.of(blocks));
    }

    private DailyMissionPlan.Block block(String type, int minutes) {
        return new DailyMissionPlan.Block(type, "title", "desc", "MODERATE", minutes);
    }
}
