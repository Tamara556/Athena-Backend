package com.athena.interview.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InterviewResultResponse(
        UUID id,
        UUID interviewId,
        int score,
        boolean passed,
        List<String> weaknesses,
        List<String> recommendations,
        Instant createdAt
) {
}
