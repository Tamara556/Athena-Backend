package com.athena.progress.service;

import com.athena.common.exception.ResourceNotFoundException;
import com.athena.progress.client.UserClient;
import com.athena.progress.dto.ProgressResponse;
import com.athena.progress.dto.ProgressUpdateRequest;
import com.athena.progress.dto.WeeklySummaryResponse;
import com.athena.progress.entity.DailyProgress;
import com.athena.progress.entity.LearningProgress;
import com.athena.progress.repository.DailyProgressRepository;
import com.athena.progress.repository.LearningProgressRepository;
import com.athena.progress.service.impl.ProgressServiceImpl;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgressServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 12);

    @Mock
    private LearningProgressRepository progressRepository;
    @Mock
    private DailyProgressRepository dailyProgressRepository;
    @Mock
    private UserClient userClient;

    private ProgressServiceImpl service;

    private final UUID userId = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-06-12T10:00:00Z"), ZoneOffset.UTC);
        service = new ProgressServiceImpl(progressRepository, dailyProgressRepository, userClient, fixedClock);
    }

    private LearningProgress progressWith(LocalDate lastActivity, int currentStreak) {
        LearningProgress p = new LearningProgress(userId);
        p.setLastActivityDate(lastActivity);
        p.setCurrentStreak(currentStreak);
        p.setLongestStreak(currentStreak);
        return p;
    }

    private void stubSaveEcho() {
        when(progressRepository.save(any(LearningProgress.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void update_firstActivity_createsProgressWithStreakOne() {
        when(progressRepository.findById(userId)).thenReturn(Optional.empty());
        when(dailyProgressRepository.findByUserIdAndDate(userId, TODAY)).thenReturn(Optional.empty());
        stubSaveEcho();

        ProgressResponse response = service.update(new ProgressUpdateRequest(userId, 3, 45));

        verify(userClient).getUser(userId); // existence verified for a brand-new user
        verify(dailyProgressRepository).save(any(DailyProgress.class));
        assertThat(response.currentStreak()).isEqualTo(1);
        assertThat(response.totalCompletedTasks()).isEqualTo(3);
        assertThat(response.totalMinutes()).isEqualTo(45);
        assertThat(response.lastActivityDate()).isEqualTo(TODAY);
    }

    @Test
    void update_consecutiveDay_incrementsStreak() {
        when(progressRepository.findById(userId)).thenReturn(Optional.of(progressWith(TODAY.minusDays(1), 3)));
        when(dailyProgressRepository.findByUserIdAndDate(userId, TODAY)).thenReturn(Optional.empty());
        stubSaveEcho();

        ProgressResponse response = service.update(new ProgressUpdateRequest(userId, 1, 10));

        assertThat(response.currentStreak()).isEqualTo(4);
        assertThat(response.longestStreak()).isEqualTo(4);
    }

    @Test
    void update_sameDay_keepsStreak() {
        when(progressRepository.findById(userId)).thenReturn(Optional.of(progressWith(TODAY, 2)));
        when(dailyProgressRepository.findByUserIdAndDate(userId, TODAY))
                .thenReturn(Optional.of(new DailyProgress(userId, TODAY)));
        stubSaveEcho();

        ProgressResponse response = service.update(new ProgressUpdateRequest(userId, 1, 10));

        assertThat(response.currentStreak()).isEqualTo(2);
    }

    @Test
    void update_gap_resetsStreak() {
        when(progressRepository.findById(userId)).thenReturn(Optional.of(progressWith(TODAY.minusDays(3), 5)));
        when(dailyProgressRepository.findByUserIdAndDate(userId, TODAY)).thenReturn(Optional.empty());
        stubSaveEcho();

        ProgressResponse response = service.update(new ProgressUpdateRequest(userId, 1, 10));

        assertThat(response.currentStreak()).isEqualTo(1);
        assertThat(response.longestStreak()).isEqualTo(5); // longest preserved
    }

    @Test
    void update_noActivity_doesNotTouchStreakOrDaily() {
        when(progressRepository.findById(userId)).thenReturn(Optional.of(progressWith(TODAY.minusDays(1), 3)));
        stubSaveEcho();

        ProgressResponse response = service.update(new ProgressUpdateRequest(userId, 0, 0));

        assertThat(response.currentStreak()).isEqualTo(3);
        assertThat(response.lastActivityDate()).isEqualTo(TODAY.minusDays(1));
        verify(dailyProgressRepository, never()).save(any());
    }

    @Test
    void update_unknownUser_throwsNotFound() {
        when(progressRepository.findById(userId)).thenReturn(Optional.empty());
        when(userClient.getUser(userId)).thenThrow(mock(FeignException.NotFound.class));

        assertThatThrownBy(() -> service.update(new ProgressUpdateRequest(userId, 1, 10)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(progressRepository, never()).save(any());
    }

    @Test
    void getProgress_missing_throwsNotFound() {
        when(progressRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProgress(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void weeklySummary_aggregatesLastSevenDays() {
        when(progressRepository.findById(userId)).thenReturn(Optional.of(progressWith(TODAY, 4)));
        DailyProgress d1 = new DailyProgress(userId, TODAY.minusDays(2));
        d1.add(2, 30);
        DailyProgress d2 = new DailyProgress(userId, TODAY);
        d2.add(5, 60);
        when(dailyProgressRepository.findByUserIdAndDateBetweenOrderByDateAsc(
                userId, TODAY.minusDays(6), TODAY)).thenReturn(List.of(d1, d2));

        WeeklySummaryResponse summary = service.weeklySummary(userId);

        assertThat(summary.weekStart()).isEqualTo(TODAY.minusDays(6));
        assertThat(summary.weekEnd()).isEqualTo(TODAY);
        assertThat(summary.totalTasksCompleted()).isEqualTo(7);
        assertThat(summary.totalMinutes()).isEqualTo(90);
        assertThat(summary.activeDays()).isEqualTo(2);
        assertThat(summary.currentStreak()).isEqualTo(4);
        assertThat(summary.days()).hasSize(2);
    }
}
