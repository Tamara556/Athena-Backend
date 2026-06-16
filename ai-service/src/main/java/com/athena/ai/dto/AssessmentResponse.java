package com.athena.ai.dto;

import java.util.List;
import java.util.UUID;

public record AssessmentResponse(UUID sessionId, List<String> questions) {
}
