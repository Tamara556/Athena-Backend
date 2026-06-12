package com.athena.auth.service;

import com.athena.auth.dto.AuthResponse;
import com.athena.auth.dto.LoginRequest;
import com.athena.auth.dto.RefreshRequest;
import com.athena.auth.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshRequest request);
}
