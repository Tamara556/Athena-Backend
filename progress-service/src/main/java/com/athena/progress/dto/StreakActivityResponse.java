package com.athena.progress.dto;

import java.time.LocalDate;
import java.util.List;

public record StreakActivityResponse(LocalDate referenceDate, List<DailyActivityResponse> days) {
}
