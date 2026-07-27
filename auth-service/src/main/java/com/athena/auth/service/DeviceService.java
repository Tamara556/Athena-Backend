package com.athena.auth.service;

import com.athena.auth.constants.AuthConstants;
import com.athena.auth.dto.DeviceResponse;
import com.athena.auth.entity.DeviceSession;
import com.athena.auth.repository.DeviceSessionRepository;
import com.athena.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

    private static final int USER_AGENT_MAX = 400;

    private final DeviceSessionRepository deviceSessionRepository;

    @Transactional
    public UUID open(UUID userId, String ipAddress, String userAgent, String refreshToken) {
        Instant now = Instant.now();
        DeviceSession session = new DeviceSession();
        session.setUserId(userId);
        session.setDeviceLabel(DeviceLabel.from(userAgent));
        session.setUserAgent(truncate(userAgent));
        session.setIpAddress(ipAddress);
        session.setRefreshTokenHash(hash(refreshToken));
        session.setCreatedAt(now);
        session.setLastSeenAt(now);
        return deviceSessionRepository.save(session).getId();
    }

    @Transactional
    public UUID touch(DeviceSession session, String refreshToken) {
        session.setLastSeenAt(Instant.now());
        session.setRefreshTokenHash(hash(refreshToken));
        return deviceSessionRepository.save(session).getId();
    }

    @Transactional(readOnly = true)
    public DeviceSession activeByRefreshToken(String refreshToken) {
        return deviceSessionRepository.findByRefreshTokenHash(hash(refreshToken))
                .filter(session -> session.getRevokedAt() == null)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<DeviceResponse> list(UUID userId, UUID currentSessionId) {
        return deviceSessionRepository.findByUserIdAndRevokedAtIsNullOrderByLastSeenAtDesc(userId).stream()
                .map(session -> new DeviceResponse(
                        session.getId(),
                        session.getDeviceLabel(),
                        session.getIpAddress(),
                        session.getCreatedAt(),
                        session.getLastSeenAt(),
                        session.getId().equals(currentSessionId)))
                .toList();
    }

    @Transactional
    public void revoke(UUID userId, UUID sessionId) {
        DeviceSession session = deviceSessionRepository.findById(sessionId)
                .filter(candidate -> candidate.getUserId().equals(userId))
                .orElseThrow(() -> ResourceNotFoundException.of(AuthConstants.DEVICE_RESOURCE_NAME, sessionId));
        if (session.getRevokedAt() == null) {
            session.setRevokedAt(Instant.now());
            deviceSessionRepository.save(session);
            log.info("Revoked device session userId={} sessionId={}", userId, sessionId);
        }
    }

    @Transactional
    public void revokeOthers(UUID userId, UUID currentSessionId) {
        Instant now = Instant.now();
        List<DeviceSession> others = deviceSessionRepository
                .findByUserIdAndRevokedAtIsNullOrderByLastSeenAtDesc(userId).stream()
                .filter(session -> !session.getId().equals(currentSessionId))
                .toList();
        others.forEach(session -> session.setRevokedAt(now));
        deviceSessionRepository.saveAll(others);
        log.info("Revoked {} other device sessions userId={}", others.size(), userId);
    }

    private String truncate(String userAgent) {
        return userAgent == null ? null : userAgent.substring(0, Math.min(userAgent.length(), USER_AGENT_MAX));
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
