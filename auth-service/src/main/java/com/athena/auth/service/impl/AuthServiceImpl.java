package com.athena.auth.service.impl;

import com.athena.auth.constants.AuthConstants;
import com.athena.auth.domain.Role;
import com.athena.auth.dto.AuthResponse;
import com.athena.auth.dto.LoginRequest;
import com.athena.auth.dto.RefreshRequest;
import com.athena.auth.dto.RegisterRequest;
import com.athena.auth.dto.TwoFactorVerifyRequest;
import com.athena.auth.entity.DeviceSession;
import com.athena.auth.entity.LoginEvent;
import com.athena.auth.entity.TwoFactorChallenge;
import com.athena.auth.entity.UserAccount;
import com.athena.auth.messaging.AuthEventPublisher;
import com.athena.auth.repository.LoginEventRepository;
import com.athena.auth.repository.TwoFactorChallengeRepository;
import com.athena.auth.repository.UserAccountRepository;
import com.athena.auth.service.AuthService;
import com.athena.auth.service.DeviceService;
import com.athena.auth.service.VerificationCodeService;
import com.athena.auth.service.UserImageService;
import com.athena.auth.sms.SmsSender;
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int USER_AGENT_MAX = 400;
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);

    private final UserAccountRepository userAccountRepository;
    private final LoginEventRepository loginEventRepository;
    private final TwoFactorChallengeRepository twoFactorChallengeRepository;
    private final VerificationCodeService verificationCodeService;
    private final SmsSender smsSender;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthEventPublisher eventPublisher;
    private final UserImageService userImageService;
    private final DeviceService deviceService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, MultipartFile image, String ipAddress, String userAgent) {
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

        return buildAuthResponse(saved, ipAddress, userAgent, null);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        String identifier = request.login().trim().toLowerCase();
        UserAccount account = userAccountRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier)
                .orElseThrow(() -> new InvalidCredentialsException(AuthConstants.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new InvalidCredentialsException(AuthConstants.INVALID_CREDENTIALS);
        }

        if (account.isTwoFactorEnabled()) {
            String code = verificationCodeService.generateCode();
            TwoFactorChallenge challenge = new TwoFactorChallenge();
            challenge.setUserId(account.getId());
            challenge.setCodeHash(verificationCodeService.hash(code));
            challenge.setExpiresAt(Instant.now().plus(CHALLENGE_TTL));
            TwoFactorChallenge saved = twoFactorChallengeRepository.save(challenge);
            smsSender.send(account.getPhoneNumber(), AuthConstants.TWO_FACTOR_SMS_MESSAGE.formatted(code));
            log.info("Login awaiting two-factor userId={}", account.getId());
            return AuthResponse.twoFactorChallenge(saved.getId().toString());
        }

        recordLogin(account.getId(), ipAddress, userAgent);
        log.info("Login success userId={}", account.getId());
        return buildAuthResponse(account, ipAddress, userAgent, null);
    }

    @Override
    @Transactional
    public AuthResponse verifyTwoFactor(TwoFactorVerifyRequest request, String ipAddress, String userAgent) {
        UUID challengeId;
        try {
            challengeId = UUID.fromString(request.challengeToken());
        } catch (IllegalArgumentException ex) {
            throw new InvalidCredentialsException(AuthConstants.TWO_FACTOR_CHALLENGE_INVALID);
        }

        TwoFactorChallenge challenge = twoFactorChallengeRepository.findById(challengeId)
                .orElseThrow(() -> new InvalidCredentialsException(AuthConstants.TWO_FACTOR_CHALLENGE_INVALID));

        if (challenge.getExpiresAt().isBefore(Instant.now())) {
            twoFactorChallengeRepository.delete(challenge);
            throw new InvalidCredentialsException(AuthConstants.TWO_FACTOR_CHALLENGE_EXPIRED);
        }

        UserAccount account = userAccountRepository.findById(challenge.getUserId())
                .orElseThrow(() -> new InvalidCredentialsException(AuthConstants.ACCOUNT_NO_LONGER_EXISTS));

        if (!account.isTwoFactorEnabled() || !verificationCodeService.matches(request.code(), challenge.getCodeHash())) {
            throw new InvalidCredentialsException(AuthConstants.TWO_FACTOR_CODE_INVALID);
        }

        twoFactorChallengeRepository.delete(challenge);
        recordLogin(account.getId(), ipAddress, userAgent);
        log.info("Two-factor verified userId={}", account.getId());
        return buildAuthResponse(account, ipAddress, userAgent, null);
    }

    private void recordLogin(UUID userId, String ipAddress, String userAgent) {
        LoginEvent event = new LoginEvent();
        event.setUserId(userId);
        event.setIpAddress(ipAddress);
        event.setUserAgent(userAgent == null ? null : userAgent.substring(0, Math.min(userAgent.length(), USER_AGENT_MAX)));
        loginEventRepository.save(event);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtService.parseAndValidate(request.refreshToken(), TokenType.REFRESH);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidCredentialsException(AuthConstants.REFRESH_TOKEN_INVALID);
        }

        DeviceSession session = deviceService.activeByRefreshToken(request.refreshToken());
        if (session == null) {
            throw new InvalidCredentialsException(AuthConstants.REFRESH_TOKEN_INVALID);
        }

        UUID userId = UUID.fromString(jwtService.extractSubject(claims));
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException(AuthConstants.ACCOUNT_NO_LONGER_EXISTS));

        log.info("Refreshed tokens userId={}", account.getId());
        return buildAuthResponse(account, null, null, session);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredImage> loadAvatar(UUID userId) {
        return userAccountRepository.findById(userId)
                .map(UserAccount::getImageName)
                .flatMap(userImageService::load);
    }

    /**
     * Issues a fresh token pair and ties it to a device session. When {@code existing}
     * is null a new session is opened (a new login); otherwise the given session is
     * kept alive with the rotated refresh token (a refresh).
     */
    private AuthResponse buildAuthResponse(UserAccount account, String ipAddress, String userAgent, DeviceSession existing) {
        String subject = account.getId().toString();
        List<String> roleNames = account.getRoles().stream().map(Enum::name).toList();
        Set<String> roleSet = account.getRoles().stream().map(Enum::name).collect(Collectors.toUnmodifiableSet());

        String accessToken = jwtService.generateAccessToken(subject, roleNames);
        String refreshToken = jwtService.generateRefreshToken(subject, roleNames);

        UUID sessionId = existing == null
                ? deviceService.open(account.getId(), ipAddress, userAgent, refreshToken)
                : deviceService.touch(existing, refreshToken);

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
                account.getImageName(),
                false,
                null,
                sessionId.toString());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase();
    }
}
