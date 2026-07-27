package com.athena.ai.dailyjourney.service.impl;

import com.athena.ai.constants.AiConstants;
import com.athena.ai.dailyjourney.dto.DailyJourneyResponse;
import com.athena.ai.dailyjourney.entity.DailyMission;
import com.athena.ai.dailyjourney.repository.AdjustmentLogRepository;
import com.athena.ai.dailyjourney.repository.DailyBlockRepository;
import com.athena.ai.dailyjourney.repository.DailyCheckinRepository;
import com.athena.ai.dailyjourney.repository.DailyMissionRepository;
import com.athena.ai.dailyjourney.repository.DailyReflectionRepository;
import com.athena.ai.dailyjourney.service.DailyJourneyDetailService;
import com.athena.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DailyJourneyDetailServiceImpl implements DailyJourneyDetailService {

    private final DailyMissionRepository missionRepository;
    private final DailyBlockRepository blockRepository;
    private final AdjustmentLogRepository adjustmentRepository;
    private final DailyCheckinRepository checkinRepository;
    private final DailyReflectionRepository reflectionRepository;
    private final WeakNodeFinder weakNodeFinder;
    private final DailyJourneyMapper mapper;

    @Override
    @Cacheable(value = AiConstants.CACHE_DAILY_JOURNEY, key = "#missionId")
    @Transactional(readOnly = true)
    public DailyJourneyResponse getDetail(UUID missionId) {
        DailyMission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> ResourceNotFoundException.of(AiConstants.RESOURCE_DAILY_MISSION, missionId));
        return mapper.toResponse(
                mission,
                blockRepository.findByMissionIdOrderByOrderIndexAsc(missionId),
                adjustmentRepository.findByMissionIdOrderByCreatedAtDesc(missionId),
                weakNodeFinder.find(mission.getUserId()),
                checkinRepository.findFirstByMissionIdOrderByCreatedAtDesc(missionId).orElse(null),
                reflectionRepository.findByMissionId(missionId).orElse(null));
    }

    @Override
    @CacheEvict(value = AiConstants.CACHE_DAILY_JOURNEY, key = "#missionId")
    public void evict(UUID missionId) {
    }
}
