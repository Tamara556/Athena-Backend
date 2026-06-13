package com.athena.badge.service;

import com.athena.badge.domain.BadgeCode;
import com.athena.badge.dto.BadgeResponse;
import com.athena.badge.entity.Badge;
import com.athena.badge.entity.UserBadge;
import com.athena.badge.repository.BadgeRepository;
import com.athena.badge.repository.UserBadgeRepository;
import com.athena.badge.service.impl.BadgeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BadgeServiceImplTest {

    @Mock
    private BadgeRepository badgeRepository;
    @Mock
    private UserBadgeRepository userBadgeRepository;

    @InjectMocks
    private BadgeServiceImpl service;

    private final UUID userId = UUID.randomUUID();

    private Badge badge(BadgeCode code) {
        Badge b = new Badge();
        b.setId(UUID.randomUUID());
        b.setCode(code.name());
        b.setName(code.name());
        return b;
    }

    @Test
    void award_grantsAllNewlyQualifiedBadges() {
        when(userBadgeRepository.findEarnedCodesByUserId(userId)).thenReturn(Set.of());
        when(badgeRepository.findByCode(BadgeCode.FIRST_TASK.name())).thenReturn(Optional.of(badge(BadgeCode.FIRST_TASK)));
        when(badgeRepository.findByCode(BadgeCode.STREAK_7.name())).thenReturn(Optional.of(badge(BadgeCode.STREAK_7)));

        List<BadgeResponse> awarded = service.award(userId, 7, 1);

        assertThat(awarded).extracting(BadgeResponse::code)
                .containsExactlyInAnyOrder("FIRST_TASK", "STREAK_7");
        verify(userBadgeRepository, times(2)).save(any(UserBadge.class));
    }

    @Test
    void award_skipsBadgesAlreadyEarned() {
        when(userBadgeRepository.findEarnedCodesByUserId(userId)).thenReturn(Set.of("FIRST_TASK"));
        when(badgeRepository.findByCode(BadgeCode.STREAK_7.name())).thenReturn(Optional.of(badge(BadgeCode.STREAK_7)));

        List<BadgeResponse> awarded = service.award(userId, 7, 1);

        assertThat(awarded).extracting(BadgeResponse::code).containsExactly("STREAK_7");
        verify(userBadgeRepository, times(1)).save(any(UserBadge.class));
    }

    @Test
    void award_doesNothingWhenNoneQualify() {
        List<BadgeResponse> awarded = service.award(userId, 0, 0);

        assertThat(awarded).isEmpty();
        verify(userBadgeRepository, never()).save(any());
        verify(userBadgeRepository, never()).findEarnedCodesByUserId(any());
    }
}
