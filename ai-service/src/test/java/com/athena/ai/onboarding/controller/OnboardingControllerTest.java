package com.athena.ai.onboarding.controller;

import com.athena.ai.onboarding.dto.StartOnboardingResponse;
import com.athena.ai.onboarding.service.OnboardingService;
import com.athena.common.security.AuthHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OnboardingController.class)
class OnboardingControllerTest {

    private static final String USER_ID = "55555555-5555-5555-5555-555555555555";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OnboardingService onboardingService;

    @Test
    void start_returns201WithGreeting() throws Exception {
        when(onboardingService.start(any())).thenReturn(
                new StartOnboardingResponse(UUID.randomUUID(), "Hello, Roza 👋", "What would you like to learn?"));

        mockMvc.perform(post("/ai/onboarding/start").header(AuthHeaders.USER_ID, USER_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.greeting").value("Hello, Roza 👋"));
    }

    @Test
    void start_returns400WhenUserHeaderMissing() throws Exception {
        mockMvc.perform(post("/ai/onboarding/start"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitGoal_returns400WhenGoalBlank() throws Exception {
        mockMvc.perform(post("/ai/onboarding/goal")
                        .header(AuthHeaders.USER_ID, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("goal"));
    }
}
