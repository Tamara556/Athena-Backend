package com.athena.interview.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InterviewResponse(
        UUID id,
        String domain,
        String level,
        String status,
        List<QuestionView> questions,
        Instant createdAt
) {

    public record QuestionView(UUID id, String type, String question) {
    }
}
