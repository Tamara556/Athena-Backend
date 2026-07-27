package com.athena.ai.dailyjourney.service.impl;

import com.athena.ai.dailyjourney.domain.BlockStatus;
import com.athena.ai.dailyjourney.dto.DailyJourneyResponse;
import com.athena.ai.dailyjourney.dto.DailyJourneyResponse.Adjustment;
import com.athena.ai.dailyjourney.dto.DailyJourneyResponse.Block;
import com.athena.ai.dailyjourney.dto.DailyJourneyResponse.Checkin;
import com.athena.ai.dailyjourney.dto.DailyJourneyResponse.Mission;
import com.athena.ai.dailyjourney.dto.DailyJourneyResponse.Progress;
import com.athena.ai.dailyjourney.dto.DailyJourneyResponse.Reflection;
import com.athena.ai.dailyjourney.dto.DailyJourneyResponse.Weakness;
import com.athena.ai.dailyjourney.entity.AdjustmentLog;
import com.athena.ai.dailyjourney.entity.DailyBlock;
import com.athena.ai.dailyjourney.entity.DailyCheckin;
import com.athena.ai.dailyjourney.entity.DailyMission;
import com.athena.ai.dailyjourney.entity.DailyReflection;
import com.athena.ai.knowledgegraph.entity.KnowledgeNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class DailyJourneyMapper {

    public DailyJourneyResponse toResponse(DailyMission mission, List<DailyBlock> blocks,
                                           List<AdjustmentLog> adjustments, List<KnowledgeNode> weakNodes,
                                           DailyCheckin lastCheckin, DailyReflection reflection) {
        Set<UUID> targetedNodes = blocks.stream()
                .map(DailyBlock::getKnowledgeNodeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        int completed = (int) blocks.stream().filter(b -> b.getStatus() == BlockStatus.COMPLETED).count();

        return new DailyJourneyResponse(
                mission.getId(),
                mission.getMissionDate(),
                mission.getLearningSessionId(),
                mission(mission),
                new Progress(completed, blocks.size()),
                blocks.stream().map(this::block).toList(),
                adjustments.stream().map(this::adjustment).toList(),
                weakNodes.stream().map(node -> weakness(node, targetedNodes)).toList(),
                lastCheckin == null ? null : checkin(lastCheckin),
                reflection(reflection));
    }

    private Mission mission(DailyMission mission) {
        return new Mission(mission.getTitle(), mission.getDescription(), mission.getGoalContext(),
                mission.getDifficulty().name(), mission.getAvailableMinutes(), mission.getEstimatedMinutes(),
                mission.getStatus().name());
    }

    private Block block(DailyBlock block) {
        return new Block(block.getId(), block.getOrderIndex(), block.getType().name(), block.getTitle(),
                block.getDescription(), block.getDifficulty().name(), block.getDurationMinutes(),
                block.getStatus().name(), block.getProgressPercent(), block.isPriorityInsert(),
                block.getSourceRef(), block.getKnowledgeNodeId(), block.getSkipReason());
    }

    private Adjustment adjustment(AdjustmentLog log) {
        return new Adjustment(log.getId(), log.getType().name(), log.getReason(), log.getAffectedBlockId(),
                log.getCreatedAt());
    }

    private Weakness weakness(KnowledgeNode node, Set<UUID> targetedNodes) {
        String source = node.getDomain() != null && !node.getDomain().isBlank() ? node.getDomain() : "Assessment";
        return new Weakness(node.getId(), node.getSkillName(), node.getDomain(), node.getMasteryPercentage(),
                source, targetedNodes.contains(node.getId()));
    }

    private Checkin checkin(DailyCheckin checkin) {
        return new Checkin(checkin.getConfidence().name(), checkin.getReply(), checkin.getCreatedAt());
    }

    private Reflection reflection(DailyReflection reflection) {
        if (reflection == null) {
            return new Reflection(false, false, null, null, null);
        }
        return new Reflection(true, reflection.isSkipped(), reflection.getHardestPart(),
                reflection.getWhatClicked(), reflection.getAdjustRequest());
    }
}
