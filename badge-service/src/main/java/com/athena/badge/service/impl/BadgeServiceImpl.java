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
}
