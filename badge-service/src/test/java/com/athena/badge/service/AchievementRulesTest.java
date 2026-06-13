package com.athena.badge.service;

import com.athena.badge.domain.BadgeCode;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AchievementRulesTest {

    @Test
    void noBadges_whenBelowAllThresholds() {
        assertThat(AchievementRules.qualifiedBadges(0, 0)).isEmpty();
    }

    @Test
    void firstTask_atOneCompletedTask() {
        assertThat(AchievementRules.qualifiedBadges(0, 1)).containsExactly(BadgeCode.FIRST_TASK);
    }

    @Test
    void taskMilestones_areCumulative() {
        assertThat(AchievementRules.qualifiedBadges(0, 50))
                .contains(BadgeCode.FIRST_TASK, BadgeCode.TASKS_10, BadgeCode.TASKS_50)
                .doesNotContain(BadgeCode.TASKS_100);
    }

    @Test
    void streakMilestones_areCumulative() {
        Set<BadgeCode> earned = AchievementRules.qualifiedBadges(30, 0);
        assertThat(earned).contains(BadgeCode.STREAK_7, BadgeCode.STREAK_14, BadgeCode.STREAK_30)
                .doesNotContain(BadgeCode.STREAK_100);
    }

    @Test
    void combinesStreakAndTaskBadges() {
        assertThat(AchievementRules.qualifiedBadges(7, 10))
                .containsExactlyInAnyOrder(BadgeCode.FIRST_TASK, BadgeCode.TASKS_10, BadgeCode.STREAK_7);
    }
}
