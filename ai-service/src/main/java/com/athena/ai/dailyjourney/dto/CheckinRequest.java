package com.athena.ai.dailyjourney.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CheckinRequest(@NotBlank String confidence, UUID blockId) {
}
