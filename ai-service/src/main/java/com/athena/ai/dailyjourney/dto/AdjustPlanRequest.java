package com.athena.ai.dailyjourney.dto;

import jakarta.validation.constraints.NotBlank;

public record AdjustPlanRequest(@NotBlank String action) {
}
