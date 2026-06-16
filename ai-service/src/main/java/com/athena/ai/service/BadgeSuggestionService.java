package com.athena.ai.service;

import com.athena.common.event.BadgeSuggestion;

import java.util.List;
import java.util.UUID;

public interface BadgeSuggestionService {

    List<BadgeSuggestion> suggest(UUID userId, String domain);
}
