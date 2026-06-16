package com.athena.common.event;

import java.time.Instant;
import java.util.UUID;

public record InterviewStartedEvent(
        UUID userId,
        UUID interviewId,
        Instant occurredAt
) {
}
