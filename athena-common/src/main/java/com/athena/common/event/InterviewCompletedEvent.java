package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

public record InterviewCompletedEvent(
        UUID userId,
        UUID interviewId,
        Instant occurredAt
) {
}
