package com.athena.auth.service;

import com.athena.auth.dto.AuthResponse;
import com.athena.auth.dto.LoginRequest;
import com.athena.auth.dto.RefreshRequest;
import com.athena.auth.dto.RegisterRequest;
import com.athena.common.storage.ImageStorage;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

public interface AuthService {

    AuthResponse register(RegisterRequest request, MultipartFile image);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshRequest request);

    Optional<ImageStorage.StoredImage> loadAvatar(UUID userId);

}
