package com.athena.ai.dailyjourney.dto;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DailyJourneyResponse(
        UUID missionId,
        LocalDate date,
        UUID learningSessionId,
        Mission mission,
        Progress progress,
        List<Block> blocks,
        List<Adjustment> adjustments,
        List<Weakness> weaknesses,
        Checkin lastCheckin,
        Reflection reflection
) implements Serializable {

    public record Mission(String title, String description, String goalContext, String difficulty,
                          int availableMinutes, int estimatedMinutes, String status) implements Serializable {
    }

    public record Progress(int completed, int total) implements Serializable {
    }

    public record Block(UUID id, int orderIndex, String type, String title, String description, String difficulty,
                        int durationMinutes, String status, int progressPercent, boolean priorityInsert,
                        UUID sourceRef, UUID knowledgeNodeId, String skipReason) implements Serializable {
    }

    public record Adjustment(UUID id, String type, String reason, UUID affectedBlockId, Instant createdAt)
            implements Serializable {
    }

    public record Weakness(UUID knowledgeNodeId, String skillName, String domain, int masteryPercentage,
                           String source, boolean inMission) implements Serializable {
    }

    public record Checkin(String confidence, String reply, Instant createdAt) implements Serializable {
    }

    public record Reflection(boolean present, boolean skipped, String hardestPart, String whatClicked,
                             String adjustRequest) implements Serializable {
    }
}
