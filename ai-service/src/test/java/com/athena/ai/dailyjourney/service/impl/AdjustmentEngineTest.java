package com.athena.ai.dailyjourney.service.impl;

import com.athena.ai.dailyjourney.domain.AdjustmentType;
import com.athena.ai.dailyjourney.domain.BlockStatus;
import com.athena.ai.dailyjourney.domain.BlockType;
import com.athena.ai.dailyjourney.domain.ConfidenceLevel;
import com.athena.ai.dailyjourney.entity.AdjustmentLog;
import com.athena.ai.dailyjourney.entity.DailyBlock;
import com.athena.ai.dailyjourney.entity.DailyMission;
import com.athena.ai.dailyjourney.repository.AdjustmentLogRepository;
import com.athena.ai.dailyjourney.repository.DailyBlockRepository;
import com.athena.ai.dailyjourney.repository.DailyCheckinRepository;
import com.athena.ai.generation.messaging.AiEventPublisher;
import com.athena.ai.generation.service.AiGenerationService;
import com.athena.ai.knowledgegraph.repository.KnowledgeNodeRepository;
import com.athena.ai.learningsession.domain.Difficulty;
import com.athena.ai.learningsession.repository.PracticeActivityRepository;
import com.athena.common.event.DailyMissionAdjustedEvent;
import com.athena.common.event.KafkaTopics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdjustmentEngineTest {

    @Mock
    private DailyBlockRepository blockRepository;
    @Mock
    private AdjustmentLogRepository adjustmentRepository;
    @Mock
    private DailyCheckinRepository checkinRepository;
    @Mock
    private KnowledgeNodeRepository knowledgeRepository;
    @Mock
    private PracticeActivityRepository practiceRepository;
    @Mock
    private AiGenerationService generation;
    @Mock
    private AiEventPublisher events;

    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    private final UUID userId = UUID.randomUUID();
    private final UUID missionId = UUID.randomUUID();

    private AdjustmentEngine engine() {
        return new AdjustmentEngine(blockRepository, adjustmentRepository, checkinRepository,
                knowledgeRepository, practiceRepository, generation, events, clock);
    }

    @Test
    void recordPersistsLogAndPublishesEvent() {
        UUID blockId = UUID.randomUUID();

        engine().record(mission(Difficulty.MODERATE), AdjustmentType.ADD, "raised challenge", blockId);

        ArgumentCaptor<AdjustmentLog> log = ArgumentCaptor.forClass(AdjustmentLog.class);
        verify(adjustmentRepository).save(log.capture());
        assertThat(log.getValue().getType()).isEqualTo(AdjustmentType.ADD);
        assertThat(log.getValue().getReason()).isEqualTo("raised challenge");
        assertThat(log.getValue().getAffectedBlockId()).isEqualTo(blockId);

        ArgumentCaptor<DailyMissionAdjustedEvent> event = ArgumentCaptor.forClass(DailyMissionAdjustedEvent.class);
        verify(events).publish(eq(KafkaTopics.DAILY_MISSION_ADJUSTED), eq(userId), event.capture());
        assertThat(event.getValue().adjustmentType()).isEqualTo("ADD");
        assertThat(event.getValue().reason()).isEqualTo("raised challenge");
        assertThat(event.getValue().affectedBlockId()).isEqualTo(blockId);
    }

    @Test
    void simplifyEasesUpcomingAndCurrentBlocksAndShrinksDuration() {
        when(blockRepository.findByMissionIdOrderByOrderIndexAsc(missionId)).thenReturn(List.of(
                block(BlockStatus.CURRENT, Difficulty.CHALLENGING, 20),
                block(BlockStatus.UPCOMING, Difficulty.MODERATE, 20),
                block(BlockStatus.COMPLETED, Difficulty.EASY, 20)));

        engine().simplify(mission(Difficulty.CHALLENGING));

        ArgumentCaptor<DailyBlock> saved = ArgumentCaptor.forClass(DailyBlock.class);
        verify(blockRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(DailyBlock::getDifficulty)
                .containsExactly(Difficulty.MODERATE, Difficulty.EASY);
        // 20 * 0.85 rounded = 17.
        assertThat(saved.getAllValues()).extracting(DailyBlock::getDurationMinutes).containsExactly(17, 17);
        verify(adjustmentRepository).save(argThatType(AdjustmentType.SIMPLIFY));
    }

    @Test
    void intensifyHardensUpcomingAndCurrentBlocks() {
        when(blockRepository.findByMissionIdOrderByOrderIndexAsc(missionId)).thenReturn(List.of(
                block(BlockStatus.CURRENT, Difficulty.EASY, 20),
                block(BlockStatus.UPCOMING, Difficulty.MODERATE, 20)));

        engine().intensify(mission(Difficulty.CHALLENGING));

        ArgumentCaptor<DailyBlock> saved = ArgumentCaptor.forClass(DailyBlock.class);
        verify(blockRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(DailyBlock::getDifficulty)
                .containsExactly(Difficulty.MODERATE, Difficulty.CHALLENGING);
        assertThat(saved.getAllValues()).extracting(DailyBlock::getDurationMinutes).containsExactly(20, 20);
        verify(adjustmentRepository).save(argThatType(AdjustmentType.ADD));
    }

    @Test
    void afterCheckinNeedHelpTriggersSimplify() {
        when(blockRepository.findByMissionIdOrderByOrderIndexAsc(missionId)).thenReturn(List.of());

        engine().afterCheckin(mission(Difficulty.MODERATE), ConfidenceLevel.NEED_HELP, null);

        verify(adjustmentRepository).save(argThatType(AdjustmentType.SIMPLIFY));
        verify(events).publish(eq(KafkaTopics.DAILY_MISSION_ADJUSTED), eq(userId), any());
    }

    @Test
    void afterCheckinConfidentMakesNoAdjustment() {
        engine().afterCheckin(mission(Difficulty.MODERATE), ConfidenceLevel.CONFIDENT, null);

        verifyNoInteractions(adjustmentRepository, events, blockRepository, generation);
    }

    private static AdjustmentLog argThatType(AdjustmentType type) {
        return org.mockito.ArgumentMatchers.argThat(log -> log.getType() == type);
    }

    private DailyMission mission(Difficulty difficulty) {
        DailyMission mission = new DailyMission(userId, LocalDate.now(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "title", "desc", "ctx", difficulty, 105, 100);
        mission.setId(missionId);
        return mission;
    }

    private DailyBlock block(BlockStatus status, Difficulty difficulty, int minutes) {
        return new DailyBlock(missionId, userId, 0, BlockType.READING, "t", "d", UUID.randomUUID(),
                null, null, difficulty, minutes, status, false);
    }
}
