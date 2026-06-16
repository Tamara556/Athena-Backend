package com.athena.common.event;

public record BadgeSuggestion(
        String code,
        String name,
        String description,
        String icon
) {
}
