package com.athena.ai.dailyjourney.dto;

import jakarta.validation.constraints.Min;

public record AdjustTimeRequest(@Min(15) int availableMinutes) {
}
