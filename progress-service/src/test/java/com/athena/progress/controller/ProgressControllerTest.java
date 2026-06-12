package com.athena.progress.controller;

import com.athena.common.exception.ResourceNotFoundException;
import com.athena.progress.dto.ProgressResponse;
import com.athena.progress.dto.WeeklySummaryResponse;
import com.athena.progress.service.ProgressService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProgressController.class)
class ProgressControllerTest {

    private static final String USER_ID = "33333333-3333-3333-3333-333333333333";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProgressService progressService;

    private ProgressResponse sampleProgress() {
        return new ProgressResponse(UUID.fromString(USER_ID), 12, 340L, 4, 7,
                LocalDate.of(2026, 6, 12), Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-06-12T00:00:00Z"));
    }

    @Test
    void getProgress_returns200() throws Exception {
        when(progressService.getProgress(eq(UUID.fromString(USER_ID)))).thenReturn(sampleProgress());

        mockMvc.perform(get("/progress/{userId}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(4))
                .andExpect(jsonPath("$.totalCompletedTasks").value(12));
    }

    @Test
    void getProgress_returns404WhenMissing() throws Exception {
        when(progressService.getProgress(any()))
                .thenThrow(ResourceNotFoundException.of("Progress", USER_ID));

        mockMvc.perform(get("/progress/{userId}", USER_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_returns200() throws Exception {
        when(progressService.update(any())).thenReturn(sampleProgress());

        mockMvc.perform(post("/progress/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","tasksCompleted":3,"minutesSpent":45}""".formatted(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID));
    }

    @Test
    void update_returns400OnNegativeValue() throws Exception {
        mockMvc.perform(post("/progress/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","tasksCompleted":-1,"minutesSpent":45}""".formatted(USER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("tasksCompleted"));
    }

    @Test
    void summary_returns200() throws Exception {
        WeeklySummaryResponse summary = new WeeklySummaryResponse(
                UUID.fromString(USER_ID), LocalDate.of(2026, 6, 6), LocalDate.of(2026, 6, 12),
                7, 90L, 2, 4,
                List.of(new WeeklySummaryResponse.DailyBreakdown(LocalDate.of(2026, 6, 12), 5, 60)));
        when(progressService.weeklySummary(eq(UUID.fromString(USER_ID)))).thenReturn(summary);

        mockMvc.perform(get("/progress/summary/{userId}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasksCompleted").value(7))
                .andExpect(jsonPath("$.days[0].minutesSpent").value(60));
    }
}
