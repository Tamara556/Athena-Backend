package com.athena.user.controller;

import com.athena.common.exception.ResourceNotFoundException;
import com.athena.user.dto.UserProfileResponse;
import com.athena.user.service.UserProfileService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    private static final String USER_ID = "22222222-2222-2222-2222-222222222222";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    private UserProfileResponse sample() {
        return new UserProfileResponse(UUID.fromString(USER_ID), "Ada", 30, "Master algorithms", 2.5,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void create_returns201WithLocation() throws Exception {
        when(userProfileService.create(any())).thenReturn(sample());

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","name":"Ada","age":30,"goal":"Master algorithms","dailyStudyHours":2.5}"""
                                .formatted(USER_ID)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/users/" + USER_ID)))
                .andExpect(jsonPath("$.userId").value(USER_ID));
    }

    @Test
    void create_returns400OnInvalidAge() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","name":"Ada","age":2,"goal":"Goal","dailyStudyHours":2.5}"""
                                .formatted(USER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("age"));
    }

    @Test
    void getById_returns200() throws Exception {
        when(userProfileService.getById(eq(UUID.fromString(USER_ID)))).thenReturn(sample());

        mockMvc.perform(get("/users/{id}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ada"));
    }

    @Test
    void getById_returns404WhenMissing() throws Exception {
        when(userProfileService.getById(any()))
                .thenThrow(ResourceNotFoundException.of("User profile", USER_ID));

        mockMvc.perform(get("/users/{id}", USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
