package com.athena.auth.controller;

import com.athena.auth.constants.AuthConstants;
import com.athena.auth.dto.AuthResponse;
import com.athena.auth.service.AuthService;
import com.athena.common.storage.ImageStorage.StoredImage;
import com.athena.common.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
                900L,
                "avatars/ada.png");
    }

    @Test
    void register_returns201WithTokens() throws Exception {
        when(authService.register(any(), any())).thenReturn(sampleResponse());

        mockMvc.perform(multipart("/auth/register")
                        .param("firstName", "Ada")
                        .param("lastName", "Lovelace")
                        .param("username", "ada")
                        .param("email", "user@example.com")
                        .param("password", "Password123!"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("ada"))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.imageName").value("avatars/ada.png"));
    }

    @Test
    void register_returns201WithUploadedImage() throws Exception {
        when(authService.register(any(), any())).thenReturn(sampleResponse());

        MockMultipartFile image = new MockMultipartFile(
                "image", "me.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/auth/register")
                        .file(image)
                        .param("firstName", "Ada")
                        .param("lastName", "Lovelace")
                        .param("username", "ada")
                        .param("email", "user@example.com")
                        .param("password", "Password123!"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageName").value("avatars/ada.png"));
    }

    @Test
    void register_returns400WhenPasswordTooShort() throws Exception {
        mockMvc.perform(multipart("/auth/register")
                        .param("firstName", "Ada")
                        .param("lastName", "Lovelace")
                        .param("username", "ada")
                        .param("email", "user@example.com")
                        .param("password", "short"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details[0].field").value("password"));
    }

    @Test
    void register_returns400WhenPasswordLacksComplexity() throws Exception {
        mockMvc.perform(multipart("/auth/register")
                        .param("firstName", "Ada")
                        .param("lastName", "Lovelace")
                        .param("username", "ada")
                        .param("email", "user@example.com")
                        .param("password", "alllowercase"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("password"));
    }

    @Test
    void register_returns400WhenUsernameInvalid() throws Exception {
        mockMvc.perform(multipart("/auth/register")
                        .param("firstName", "Ada")
                        .param("lastName", "Lovelace")
                        .param("username", "a b")
                        .param("email", "user@example.com")
                        .param("password", "Password123!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("username"));
    }

    @Test
    void avatar_returnsImageBytes() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        byte[] data = {1, 2, 3, 4};
        when(authService.loadAvatar(eq(userId)))
                .thenReturn(Optional.of(new StoredImage(data, MediaType.IMAGE_PNG_VALUE)));

        mockMvc.perform(get("/auth/users/{userId}/image", userId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE))
                .andExpect(content().bytes(data));
    }

    @Test
    void avatar_returns404WhenNoImage() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(authService.loadAvatar(eq(userId))).thenReturn(Optional.empty());

        mockMvc.perform(get("/auth/users/{userId}/image", userId))
                .andExpect(status().isNotFound());
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
        when(authService.login(any())).thenThrow(new InvalidCredentialsException(AuthConstants.INVALID_CREDENTIALS));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"user@example.com","password":"Password123!"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
