package com.athena.auth.service.impl;

import com.athena.auth.domain.Role;
import com.athena.auth.dto.AuthResponse;
import com.athena.auth.dto.LoginRequest;
import com.athena.auth.dto.RefreshRequest;
import com.athena.auth.dto.RegisterRequest;
import com.athena.auth.entity.UserAccount;
import com.athena.auth.repository.UserAccountRepository;
import com.athena.auth.service.AuthService;
import com.athena.common.exception.DuplicateResourceException;
import com.athena.common.exception.InvalidCredentialsException;
import com.athena.common.security.JwtService;
import com.athena.common.security.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid login or password";

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        String username = normalizeUsername(request.username());

        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        if (userAccountRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateResourceException("This username is already taken");
        }

        UserAccount account = new UserAccount();
        account.setFirstName(request.firstName().trim());
        account.setLastName(request.lastName().trim());
        account.setUsername(username);
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.addRole(Role.USER);

        UserAccount saved = userAccountRepository.save(account);
        log.info("Registered new account userId={} username={}", saved.getId(), username);
        return buildAuthResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String identifier = request.login().trim().toLowerCase();
        UserAccount account = userAccountRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier)
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new InvalidCredentialsException(INVALID_CREDENTIALS);
        }

        log.info("Login success userId={}", account.getId());
        return buildAuthResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtService.parseAndValidate(request.refreshToken(), TokenType.REFRESH);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidCredentialsException("Refresh token is invalid or expired");
        }

        UUID userId = UUID.fromString(jwtService.extractSubject(claims));
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("Account no longer exists"));

        log.info("Refreshed tokens userId={}", account.getId());
        return buildAuthResponse(account);
    }

    private AuthResponse buildAuthResponse(UserAccount account) {
        String subject = account.getId().toString();
        List<String> roleNames = account.getRoles().stream().map(Enum::name).toList();
        Set<String> roleSet = account.getRoles().stream().map(Enum::name).collect(Collectors.toUnmodifiableSet());

        String accessToken = jwtService.generateAccessToken(subject, roleNames);
        String refreshToken = jwtService.generateRefreshToken(subject, roleNames);

        return new AuthResponse(
                account.getId(),
                account.getUsername(),
                account.getFirstName(),
                account.getLastName(),
                account.getEmail(),
                roleSet,
                "Bearer",
                accessToken,
                refreshToken,
                jwtService.getAccessTokenTtlSeconds());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase();
    }
}
