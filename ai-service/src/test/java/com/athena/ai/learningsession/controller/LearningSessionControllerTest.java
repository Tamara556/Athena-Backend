package com.athena.ai.learningsession.controller;

import com.athena.ai.learningsession.dto.LearningSessionResponse;
import com.athena.ai.learningsession.dto.LearningSessionSummaryResponse;
import com.athena.ai.learningsession.service.LearningSessionService;
import com.athena.common.security.AuthHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LearningSessionController.class)
class LearningSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LearningSessionService learningSessionService;

    private final UUID userId = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private final UUID sessionId = UUID.fromString("77777777-7777-7777-7777-777777777777");

    private LearningSessionResponse sample() {
        return new LearningSessionResponse(sessionId, UUID.randomUUID(), UUID.randomUUID(), 0,
                "Foundations", "NOT_STARTED", 60, Instant.parse("2026-06-17T00:00:00Z"),
                Instant.parse("2026-06-17T00:00:00Z"), List.of(), List.of(), List.of(), List.of());
    }

    @Test
    void current_returnsSession() throws Exception {
        when(learningSessionService.getCurrent(userId)).thenReturn(sample());

        mockMvc.perform(get("/learning-sessions/current").header(AuthHeaders.USER_ID, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId.toString()))
                .andExpect(jsonPath("$.status").value("NOT_STARTED"));
    }

    @Test
    void upcoming_returnsSummaries() throws Exception {
        when(learningSessionService.getUpcoming(userId)).thenReturn(List.of(
                new LearningSessionSummaryResponse(sessionId, UUID.randomUUID(), 1, "Phase 1", "NOT_STARTED", 45)));

        mockMvc.perform(get("/learning-sessions/upcoming").header(AuthHeaders.USER_ID, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nodeIndex").value(1))
                .andExpect(jsonPath("$[0].title").value("Phase 1"));
    }

    @Test
    void start_delegatesToService() throws Exception {
        when(learningSessionService.start(userId, sessionId)).thenReturn(sample());

        mockMvc.perform(post("/learning-sessions/{id}/start", sessionId).header(AuthHeaders.USER_ID, userId))
                .andExpect(status().isOk());

        verify(learningSessionService).start(eq(userId), eq(sessionId));
    }

    @Test
    void complete_delegatesToService() throws Exception {
        when(learningSessionService.complete(userId, sessionId)).thenReturn(sample());

        mockMvc.perform(post("/learning-sessions/{id}/complete", sessionId).header(AuthHeaders.USER_ID, userId))
                .andExpect(status().isOk());

        verify(learningSessionService).complete(eq(userId), eq(sessionId));
    }
}
