package com.athena.auth.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;

@Component
public class VerificationCodeService {

    public static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final int CODE_DIGITS = 6;
    private static final int CODE_BOUND = 1_000_000; // 6 digits: 000000-999999

    private final SecureRandom random = new SecureRandom();

    public String generateCode() {
        return String.format("%0" + CODE_DIGITS + "d", random.nextInt(CODE_BOUND));
    }

    public String hash(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(code.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    public boolean matches(String code, String expectedHash) {
        if (code == null || expectedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(
                hash(code.trim()).getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8));
    }
}
