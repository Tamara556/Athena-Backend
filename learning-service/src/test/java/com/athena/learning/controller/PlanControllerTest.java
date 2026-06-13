package com.athena.learning.controller;

import com.athena.common.security.AuthHeaders;
import com.athena.learning.dto.PlanResponse;
import com.athena.learning.service.PlanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlanController.class)
class PlanControllerTest {

    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlanService planService;

    private PlanResponse sample() {
        return new PlanResponse(UUID.randomUUID(), UUID.fromString(USER_ID), "Java mastery",
                "Become fluent", Instant.parse("2026-06-12T10:00:00Z"));
    }

    @Test
    void create_returns201() throws Exception {
        when(planService.create(eq(UUID.fromString(USER_ID)), any())).thenReturn(sample());

        mockMvc.perform(post("/plans")
                        .header(AuthHeaders.USER_ID, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Java mastery","description":"Become fluent"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Java mastery"));
    }

    @Test
    void create_returns400WhenUserHeaderMissing() throws Exception {
        mockMvc.perform(post("/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Java mastery"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns400WhenTitleBlank() throws Exception {
        mockMvc.perform(post("/plans")
                        .header(AuthHeaders.USER_ID, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("title"));
    }
}
