package com.athena.badge.service.impl;

import com.athena.badge.domain.BadgeCode;
import com.athena.badge.dto.BadgeResponse;
import com.athena.badge.dto.UserBadgeResponse;
import com.athena.badge.entity.Badge;
import com.athena.badge.entity.UserBadge;
import com.athena.badge.mapper.BadgeMapper;
import com.athena.badge.repository.BadgeRepository;
import com.athena.badge.repository.UserBadgeRepository;
import com.athena.badge.service.AchievementRules;
import com.athena.badge.service.BadgeService;
import com.athena.badge.service.BadgeSuggestionValidator;
import com.athena.common.event.BadgeSuggestion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeServiceImpl implements BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;

    @Override
    @Cacheable("badge-catalogue")
    @Transactional(readOnly = true)
    public List<BadgeResponse> getAllBadges() {
        return badgeRepository.findAll().stream().map(BadgeMapper::toResponse).toList();
    }

    @Override
    @Cacheable(value = "user-badges", key = "#userId")
    @Transactional(readOnly = true)
    public List<UserBadgeResponse> getUserBadges(UUID userId) {
        return userBadgeRepository.findByUserIdOrderByAwardedAtAsc(userId).stream()
                .map(BadgeMapper::toResponse)
                .toList();
    }

    @Override
    @CacheEvict(value = "user-badges", key = "#userId")
    @Transactional
    public List<BadgeResponse> award(UUID userId, int currentStreak, int completedTasks) {
        Set<BadgeCode> qualified = AchievementRules.qualifiedBadges(currentStreak, completedTasks);
        if (qualified.isEmpty()) {
            return List.of();
        }

        Set<String> alreadyEarned = userBadgeRepository.findEarnedCodesByUserId(userId);
        List<BadgeResponse> newlyAwarded = new ArrayList<>();

        qualified.forEach(code -> {
            if (alreadyEarned.contains(code.name())) {
                return;
            }
            Badge badge = badgeRepository.findByCode(code.name()).orElse(null);
            if (badge == null) {
                log.warn("No badge row seeded for code={}; skipping", code);
                return;
            }
            userBadgeRepository.save(new UserBadge(userId, badge));
            newlyAwarded.add(BadgeMapper.toResponse(badge));
            log.info("Awarded badge {} to userId={}", code, userId);
        });
        return newlyAwarded;
    }

    @Override
    @CacheEvict(value = "user-badges", key = "#userId")
    @Transactional
    public List<BadgeResponse> awardSuggested(UUID userId, List<BadgeSuggestion> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return List.of();
        }
        Set<String> alreadyEarned = userBadgeRepository.findEarnedCodesByUserId(userId);
        List<BadgeResponse> awarded = new ArrayList<>();

        for (BadgeSuggestion suggestion : suggestions) {
            if (!BadgeSuggestionValidator.isValid(suggestion)) {
                log.warn("Rejected invalid AI badge suggestion code={}",
                        suggestion == null ? null : suggestion.code());
                continue;
            }
            if (alreadyEarned.contains(suggestion.code())) {
                continue;
            }
            Badge badge = badgeRepository.findByCode(suggestion.code())
                    .orElseGet(() -> badgeRepository.save(fromSuggestion(suggestion)));
            userBadgeRepository.save(new UserBadge(userId, badge));
            awarded.add(BadgeMapper.toResponse(badge));
            log.info("Awarded AI-suggested badge {} to userId={}", suggestion.code(), userId);
        }
        return awarded;
    }

    private Badge fromSuggestion(BadgeSuggestion suggestion) {
        Badge badge = new Badge();
        badge.setCode(suggestion.code());
        badge.setName(suggestion.name());
        badge.setDescription(suggestion.description());
        badge.setIcon(suggestion.icon());
        return badge;
    }
}
