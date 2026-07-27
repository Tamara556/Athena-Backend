package com.athena.ai.dailyjourney.service;

import com.athena.ai.dailyjourney.domain.AdjustAction;
import com.athena.ai.dailyjourney.domain.ConfidenceLevel;
import com.athena.ai.dailyjourney.dto.DailyJourneyResponse;
import com.athena.ai.generation.model.WhyReasoning;

import java.util.UUID;

public interface DailyJourneyService {

    DailyJourneyResponse getToday(UUID userId);

    DailyJourneyResponse startDay(UUID userId);

    WhyReasoning getWhy(UUID userId);

    DailyJourneyResponse adjustPlan(UUID userId, AdjustAction action);

    DailyJourneyResponse adjustTime(UUID userId, int availableMinutes);

    DailyJourneyResponse startBlock(UUID userId, UUID blockId);

    DailyJourneyResponse updateProgress(UUID userId, UUID blockId, int percent);

    DailyJourneyResponse completeBlock(UUID userId, UUID blockId);

    DailyJourneyResponse skipBlock(UUID userId, UUID blockId, String reason);

    DailyJourneyResponse relinkBlock(UUID userId, UUID blockId);

    DailyJourneyResponse strengthen(UUID userId, UUID knowledgeNodeId);

    DailyJourneyResponse checkin(UUID userId, ConfidenceLevel confidence, UUID blockId);

    DailyJourneyResponse saveReflection(UUID userId, String hardestPart, String whatClicked, String adjustRequest);

    DailyJourneyResponse skipReflection(UUID userId);
}
