package com.athena.ai.generation.model;

import com.athena.common.event.BadgeSuggestion;

import java.util.List;

public record BadgeSuggestions(List<BadgeSuggestion> badges) {
}
