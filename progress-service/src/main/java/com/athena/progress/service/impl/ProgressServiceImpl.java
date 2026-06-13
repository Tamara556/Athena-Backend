package com.athena.progress.service.impl;

import com.athena.common.exception.ResourceNotFoundException;
import com.athena.progress.client.UserClient;
import com.athena.progress.dto.ProgressResponse;
import com.athena.progress.dto.ProgressUpdateRequest;
import com.athena.progress.dto.WeeklySummaryResponse;
import com.athena.progress.entity.DailyProgress;
import com.athena.progress.entity.LearningProgress;
import com.athena.progress.repository.DailyProgressRepository;
import com.athena.progress.repository.LearningProgressRepository;
import com.athena.progress.service.ProgressService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressServiceImpl implements ProgressService {

    private static final int WEEK_DAYS = 7;
    private static final String RESOURCE = "Progress";

    private final LearningProgressRepository progressRepository;
    private final DailyProgressRepository dailyProgressRepository;
    private final UserClient userClient;
    private final Clock clock;

    @Override
    @Cacheable(value = "progress", key = "#userId")
    @Transactional(readOnly = true)
    public ProgressResponse getProgress(UUID userId) {
        return progressRepository.findById(userId)
                .map(this::toResponse)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, userId));
    }

    @Override
    @CacheEvict(value = "progress", key = "#request.userId()")
    @Transactional
    public ProgressResponse update(ProgressUpdateRequest request) {
        LocalDate today = LocalDate.now(clock);
        LearningProgress progress = progressRepository.findById(request.userId())
                .orElseGet(() -> createForNewUser(request.userId()));

        progress.setTotalCompletedTasks(progress.getTotalCompletedTasks() + request.tasksCompleted());
        progress.setTotalMinutes(progress.getTotalMinutes() + request.minutesSpent());

        if (request.hasActivity()) {
            recordDailyActivity(request, today);
            applyStreak(progress, today);
            progress.setLastActivityDate(today);
        }

        LearningProgress saved = progressRepository.save(progress);
        log.info("Updated progress userId={} streak={} totalTasks={}",
                saved.getUserId(), saved.getCurrentStreak(), saved.getTotalCompletedTasks());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public WeeklySummaryResponse weeklySummary(UUID userId) {
        LearningProgress progress = progressRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, userId));

        LocalDate today = LocalDate.now(clock);
        LocalDate weekStart = today.minusDays(WEEK_DAYS - 1L);
        List<DailyProgress> entries =
                dailyProgressRepository.findByUserIdAndDateBetweenOrderByDateAsc(userId, weekStart, today);

        int totalTasks = entries.stream().mapToInt(DailyProgress::getTasksCompleted).sum();
        long totalMinutes = entries.stream().mapToLong(DailyProgress::getMinutesSpent).sum();
        List<WeeklySummaryResponse.DailyBreakdown> days = entries.stream()
                .map(e -> new WeeklySummaryResponse.DailyBreakdown(e.getDate(), e.getTasksCompleted(), e.getMinutesSpent()))
                .toList();

        return new WeeklySummaryResponse(
                userId, weekStart, today, totalTasks, totalMinutes, days.size(),
                progress.getCurrentStreak(), days);
    }

    private LearningProgress createForNewUser(UUID userId) {
        verifyUserExists(userId);
        return new LearningProgress(userId);
    }

    private void verifyUserExists(UUID userId) {
        try {
            userClient.getUser(userId);
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Cannot track progress: user " + userId + " does not exist");
        }
    }

    private void recordDailyActivity(ProgressUpdateRequest request, LocalDate today) {
        DailyProgress daily = dailyProgressRepository.findByUserIdAndDate(request.userId(), today)
                .orElseGet(() -> new DailyProgress(request.userId(), today));
        daily.add(request.tasksCompleted(), request.minutesSpent());
        dailyProgressRepository.save(daily);
    }

    private void applyStreak(LearningProgress progress, LocalDate today) {
        LocalDate last = progress.getLastActivityDate();
        int streak;
        if (last == null) {
            streak = 1;
        } else if (last.equals(today)) {
            streak = Math.max(progress.getCurrentStreak(), 1);
        } else if (last.equals(today.minusDays(1))) {
            streak = progress.getCurrentStreak() + 1;
        } else {
            streak = 1;
        }
        progress.setCurrentStreak(streak);
        progress.setLongestStreak(Math.max(progress.getLongestStreak(), streak));
    }

    private ProgressResponse toResponse(LearningProgress learningProgress) {
        return new ProgressResponse(
                learningProgress.getUserId(),
                learningProgress.getTotalCompletedTasks(),
                learningProgress.getTotalMinutes(),
                learningProgress.getCurrentStreak(),
                learningProgress.getLongestStreak(),
                learningProgress.getLastActivityDate(),
                learningProgress.getCreatedAt(),
                learningProgress.getUpdatedAt());
    }
}
