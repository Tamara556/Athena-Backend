package com.athena.badge.service;

import com.athena.badge.dto.BadgeResponse;
import com.athena.badge.dto.UserBadgeResponse;
import com.athena.common.event.BadgeSuggestion;

import java.util.List;
import java.util.UUID;

public interface BadgeService {

    List<BadgeResponse> getAllBadges();

    List<UserBadgeResponse> getUserBadges(UUID userId);

    List<BadgeResponse> award(UUID userId, int currentStreak, int completedTasks);

    List<BadgeResponse> awardSuggested(UUID userId, List<BadgeSuggestion> suggestions);
}
