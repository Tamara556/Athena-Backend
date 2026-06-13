package com.athena.badge.service;

import com.athena.badge.domain.BadgeCode;

import java.util.EnumSet;
import java.util.Set;

public final class AchievementRules {

    private AchievementRules() {
    }

    public static Set<BadgeCode> qualifiedBadges(int currentStreak, int completedTasks) {
        Set<BadgeCode> earned = EnumSet.noneOf(BadgeCode.class);

        if (completedTasks >= 1) earned.add(BadgeCode.FIRST_TASK);
        if (completedTasks >= 10) earned.add(BadgeCode.TASKS_10);
        if (completedTasks >= 50) earned.add(BadgeCode.TASKS_50);
        if (completedTasks >= 100) earned.add(BadgeCode.TASKS_100);

        if (currentStreak >= 7) earned.add(BadgeCode.STREAK_7);
        if (currentStreak >= 14) earned.add(BadgeCode.STREAK_14);
        if (currentStreak >= 30) earned.add(BadgeCode.STREAK_30);
        if (currentStreak >= 100) earned.add(BadgeCode.STREAK_100);

        return earned;
    }
}
