package com.athena.ai.dailyjourney.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record BlockProgressRequest(@Min(0) @Max(100) int percent) {
}
