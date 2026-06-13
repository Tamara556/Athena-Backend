package com.athena.auth.controller;

import com.athena.auth.dto.AuthResponse;
import com.athena.auth.service.AuthService;
import com.athena.common.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    private AuthResponse sampleResponse() {
        return new AuthResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "ada",
                "Ada",
                "Lovelace",
                "user@example.com",
                Set.of("USER"),
                "Bearer",
                "access-token",
                "refresh-token",
                900L);
    }

    @Test
    void register_returns201WithTokens() throws Exception {
        when(authService.register(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ada","lastName":"Lovelace","username":"ada","email":"user@example.com","password":"Password123!"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("ada"))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void register_returns400WhenPasswordTooShort() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ada","lastName":"Lovelace","username":"ada","email":"user@example.com","password":"short"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details[0].field").value("password"));
    }

    @Test
    void register_returns400WhenPasswordLacksComplexity() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ada","lastName":"Lovelace","username":"ada","email":"user@example.com","password":"alllowercase"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("password"));
    }

    @Test
    void register_returns400WhenUsernameInvalid() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ada","lastName":"Lovelace","username":"a b","email":"user@example.com","password":"Password123!"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("username"));
    }

    @Test
    void login_returns200() throws Exception {
        when(authService.login(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"ada","password":"Password123!"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("11111111-1111-1111-1111-111111111111"));
    }

    @Test
    void login_returns401OnInvalidCredentials() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException("Invalid login or password"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"user@example.com","password":"Password123!"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
