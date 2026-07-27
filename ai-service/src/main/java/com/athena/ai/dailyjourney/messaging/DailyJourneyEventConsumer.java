package com.athena.ai.dailyjourney.messaging;

import com.athena.ai.constants.AiConstants;
import com.athena.ai.dailyjourney.domain.BlockStatus;
import com.athena.ai.dailyjourney.domain.ConfidenceLevel;
import com.athena.ai.dailyjourney.entity.DailyBlock;
import com.athena.ai.dailyjourney.repository.DailyBlockRepository;
import com.athena.ai.dailyjourney.repository.DailyMissionRepository;
import com.athena.ai.knowledgegraph.repository.KnowledgeNodeRepository;
import com.athena.ai.dailyjourney.service.DailyJourneyDetailService;
import com.athena.ai.dailyjourney.service.impl.AdjustmentEngine;
import com.athena.common.event.DailyBlockCompletedEvent;
import com.athena.common.event.DailyCheckinRecordedEvent;
import com.athena.common.event.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyJourneyEventConsumer {

    private final DailyMissionRepository missionRepository;
    private final DailyBlockRepository blockRepository;
    private final KnowledgeNodeRepository knowledgeRepository;
    private final AdjustmentEngine adjustmentEngine;
    private final DailyJourneyDetailService detail;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.DAILY_BLOCK_COMPLETED, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onBlockCompleted(String payload) {
        DailyBlockCompletedEvent event = objectMapper.readValue(payload, DailyBlockCompletedEvent.class);
        bumpMastery(event.knowledgeNodeId());
        missionRepository.findById(event.missionId()).ifPresent(mission ->
                blockRepository.findById(event.blockId()).ifPresent(block -> {
                    adjustmentEngine.afterBlockCompleted(mission, block);
                    detail.evict(mission.getId());
                }));
    }

    @KafkaListener(topics = KafkaTopics.DAILY_CHECKIN_RECORDED, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onCheckinRecorded(String payload) {
        DailyCheckinRecordedEvent event = objectMapper.readValue(payload, DailyCheckinRecordedEvent.class);
        missionRepository.findById(event.missionId()).ifPresent(mission -> {
            DailyBlock current = blockRepository.findByMissionIdOrderByOrderIndexAsc(mission.getId()).stream()
                    .filter(block -> block.getStatus() == BlockStatus.CURRENT)
                    .findFirst()
                    .orElse(null);
            adjustmentEngine.afterCheckin(mission, ConfidenceLevel.fromString(event.confidence()), current);
            detail.evict(mission.getId());
        });
    }

    private void bumpMastery(UUID knowledgeNodeId) {
        if (knowledgeNodeId == null) {
            return;
        }
        knowledgeRepository.findById(knowledgeNodeId).ifPresent(node -> {
            node.setMasteryPercentage(Math.min(100, node.getMasteryPercentage() + AiConstants.MASTERY_DRILL_DELTA));
            knowledgeRepository.save(node);
        });
    }
}
