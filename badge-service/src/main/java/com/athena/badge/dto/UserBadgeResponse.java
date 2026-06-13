package com.athena.badge.dto;

import java.io.Serializable;
import java.time.Instant;

public record UserBadgeResponse(
        String code,
        String name,
        String description,
        String icon,
        Instant awardedAt
) implements Serializable {
}
