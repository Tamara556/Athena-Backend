package com.athena.progress.service;

import com.athena.progress.dto.ProgressResponse;
import com.athena.progress.dto.ProgressUpdateRequest;
import com.athena.progress.dto.WeeklySummaryResponse;

import java.util.UUID;

public interface ProgressService {

    ProgressResponse getProgress(UUID userId);

    ProgressResponse update(ProgressUpdateRequest request);

    WeeklySummaryResponse weeklySummary(UUID userId);
}
