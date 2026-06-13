package com.athena.badge.service;

import com.athena.badge.constants.BadgeConstants;
import com.athena.badge.domain.BadgeCode;

import java.util.EnumSet;
import java.util.Set;

public final class AchievementRules {

    private AchievementRules() {
    }

    public static Set<BadgeCode> qualifiedBadges(int currentStreak, int completedTasks) {
        Set<BadgeCode> earned = EnumSet.noneOf(BadgeCode.class);

        if (completedTasks >= BadgeConstants.FIRST_TASK_THRESHOLD) earned.add(BadgeCode.FIRST_TASK);
        if (completedTasks >= BadgeConstants.TASKS_10_THRESHOLD) earned.add(BadgeCode.TASKS_10);
        if (completedTasks >= BadgeConstants.TASKS_50_THRESHOLD) earned.add(BadgeCode.TASKS_50);
        if (completedTasks >= BadgeConstants.TASKS_100_THRESHOLD) earned.add(BadgeCode.TASKS_100);

        if (currentStreak >= BadgeConstants.STREAK_7_THRESHOLD) earned.add(BadgeCode.STREAK_7);
        if (currentStreak >= BadgeConstants.STREAK_14_THRESHOLD) earned.add(BadgeCode.STREAK_14);
        if (currentStreak >= BadgeConstants.STREAK_30_THRESHOLD) earned.add(BadgeCode.STREAK_30);
        if (currentStreak >= BadgeConstants.STREAK_100_THRESHOLD) earned.add(BadgeCode.STREAK_100);

        return earned;
    }
}
