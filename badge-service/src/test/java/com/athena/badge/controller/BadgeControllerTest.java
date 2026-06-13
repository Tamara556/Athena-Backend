package com.athena.badge.controller;

import com.athena.badge.dto.BadgeResponse;
import com.athena.badge.dto.UserBadgeResponse;
import com.athena.badge.service.BadgeService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BadgeController.class)
class BadgeControllerTest {

    private static final String USER_ID = "44444444-4444-4444-4444-444444444444";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BadgeService badgeService;

    @Test
    void allBadges_returns200() throws Exception {
        when(badgeService.getAllBadges()).thenReturn(List.of(
                new BadgeResponse("FIRST_TASK", "First Task", "Complete your first task", "🎯")));

        mockMvc.perform(get("/badges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("FIRST_TASK"));
    }

    @Test
    void myBadges_returns200() throws Exception {
        when(badgeService.getUserBadges(eq(UUID.fromString(USER_ID)))).thenReturn(List.of(
                new UserBadgeResponse("STREAK_7", "7-Day Streak", "Learn 7 days in a row", "🔥",
                        Instant.parse("2026-06-12T10:00:00Z"))));

        mockMvc.perform(get("/badges/me").header(AuthHeaders.USER_ID, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("STREAK_7"));
    }

    @Test
    void myBadges_returns400WithoutUserHeader() throws Exception {
        mockMvc.perform(get("/badges/me"))
                .andExpect(status().isBadRequest());
    }
}
