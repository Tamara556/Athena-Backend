package com.athena.badge.mapper;

import com.athena.badge.dto.BadgeResponse;
import com.athena.badge.dto.UserBadgeResponse;
import com.athena.badge.entity.Badge;
import com.athena.badge.entity.UserBadge;

public final class BadgeMapper {

    public static BadgeResponse toResponse(Badge badge) {
        return new BadgeResponse(badge.getCode(), badge.getName(), badge.getDescription(), badge.getIcon());
    }

    public static UserBadgeResponse toResponse(UserBadge userBadge) {
        Badge badge = userBadge.getBadge();
        return new UserBadgeResponse(badge.getCode(), badge.getName(), badge.getDescription(),
                badge.getIcon(), userBadge.getAwardedAt());
    }
}
