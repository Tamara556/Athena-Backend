package com.athena.badge.dto;

import java.io.Serializable;

public record BadgeResponse(
        String code,
        String name,
        String description,
        String icon
) implements Serializable {
}
