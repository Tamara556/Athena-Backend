package com.athena.common.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InterviewEvaluatedEvent(
        UUID userId,
        UUID interviewId,
        String domain,
        int score,
        boolean passed,
        List<String> weaknesses,
        Instant occurredAt
) {
}
