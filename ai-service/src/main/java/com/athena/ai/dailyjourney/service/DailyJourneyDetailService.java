package com.athena.ai.dailyjourney.service;

import com.athena.ai.dailyjourney.dto.DailyJourneyResponse;

import java.util.UUID;

public interface DailyJourneyDetailService {

    DailyJourneyResponse getDetail(UUID missionId);

    void evict(UUID missionId);
}
