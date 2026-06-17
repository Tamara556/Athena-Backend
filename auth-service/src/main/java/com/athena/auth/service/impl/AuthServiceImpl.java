package com.athena.auth.service.impl;

import com.athena.auth.constants.AuthConstants;
import com.athena.auth.domain.Role;
import com.athena.auth.dto.AuthResponse;
import com.athena.auth.dto.LoginRequest;
import com.athena.auth.dto.RefreshRequest;
import com.athena.auth.dto.RegisterRequest;
import com.athena.auth.entity.UserAccount;
import com.athena.auth.messaging.AuthEventPublisher;
import com.athena.auth.repository.UserAccountRepository;
import com.athena.auth.service.AuthService;
import com.athena.auth.service.UserImageService;
import com.athena.common.event.UserRegisteredEvent;
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
import org.springframework.web.multipart.MultipartFile;

import com.athena.common.storage.ImageStorage.StoredImage;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthEventPublisher eventPublisher;
    private final UserImageService userImageService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, MultipartFile image) {
        String email = normalizeEmail(request.email());
        String username = normalizeUsername(request.username());

        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException(AuthConstants.EMAIL_ALREADY_EXISTS);
        }
        if (userAccountRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateResourceException(AuthConstants.USERNAME_ALREADY_TAKEN);
        }

        UserAccount account = new UserAccount();
        account.setFirstName(request.firstName().trim());
        account.setLastName(request.lastName().trim());
        account.setUsername(username);
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.addRole(Role.USER);

        UserAccount saved = userAccountRepository.save(account);
        String imageName = userImageService.store(saved.getId(), image);
        if (imageName != null) {
            saved.setImageName(imageName);
            saved = userAccountRepository.save(saved);
        }
        log.info("Registered new account userId={} username={}", saved.getId(), username);

        eventPublisher.publishUserRegistered(new UserRegisteredEvent(
                saved.getId(), saved.getFirstName(), saved.getLastName(), saved.getEmail(), java.time.Instant.now()));

        return buildAuthResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String identifier = request.login().trim().toLowerCase();
        UserAccount account = userAccountRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier)
                .orElseThrow(() -> new InvalidCredentialsException(AuthConstants.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new InvalidCredentialsException(AuthConstants.INVALID_CREDENTIALS);
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
            throw new InvalidCredentialsException(AuthConstants.REFRESH_TOKEN_INVALID);
        }

        UUID userId = UUID.fromString(jwtService.extractSubject(claims));
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException(AuthConstants.ACCOUNT_NO_LONGER_EXISTS));

        log.info("Refreshed tokens userId={}", account.getId());
        return buildAuthResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredImage> loadAvatar(UUID userId) {
        return userAccountRepository.findById(userId)
                .map(UserAccount::getImageName)
                .flatMap(userImageService::load);
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
                jwtService.getAccessTokenTtlSeconds(),
                account.getImageName());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase();
    }
}
