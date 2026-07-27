package com.athena.progress.dto;

import java.time.LocalDate;

public record DailyActivityResponse(LocalDate date, int tasks, int minutes, int interviews) {
}
