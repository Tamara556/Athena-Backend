package com.athena.auth.service;

import com.athena.auth.client.AiExportClient;
import com.athena.auth.client.BadgeExportClient;
import com.athena.auth.client.InterviewExportClient;
import com.athena.auth.client.ProgressExportClient;
import com.athena.auth.client.UserExportClient;
import com.athena.auth.constants.AuthConstants;
import com.athena.auth.entity.UserAccount;
import com.athena.auth.repository.LoginEventRepository;
import com.athena.auth.repository.UserAccountRepository;
import com.athena.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataExportService {

    private final UserAccountRepository userAccountRepository;
    private final LoginEventRepository loginEventRepository;
    private final AiExportClient aiExportClient;
    private final InterviewExportClient interviewExportClient;
    private final ProgressExportClient progressExportClient;
    private final BadgeExportClient badgeExportClient;
    private final UserExportClient userExportClient;

    @Transactional(readOnly = true)
    public Map<String, Object> export(UUID userId) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of(AuthConstants.ACCOUNT_RESOURCE_NAME, userId));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exportedAt", Instant.now().toString());
        data.put("account", accountSection(account));
        data.put("security", securitySection(account));
        data.put("settings", section("settings", () -> userExportClient.settings(userId)));

        Map<String, Object> learning = new LinkedHashMap<>();
        learning.put("roadmap", section("roadmap", () -> aiExportClient.roadmap(userId)));
        learning.put("dailyPlan", section("dailyPlan", () -> aiExportClient.dailyPlan(userId)));
        data.put("learning", learning);

        Map<String, Object> progress = new LinkedHashMap<>();
        progress.put("summary", section("progress", () -> progressExportClient.progress(userId)));
        progress.put("streaks", section("streaks", () -> progressExportClient.streaks(userId)));
        data.put("progress", progress);

        data.put("interviews", section("interviews", () -> interviewExportClient.interviews(userId)));
        data.put("badges", section("badges", () -> badgeExportClient.badges(userId)));

        log.info("Generated data export userId={}", userId);
        return data;
    }

    private Map<String, Object> accountSection(UserAccount account) {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("id", account.getId());
        section.put("firstName", account.getFirstName());
        section.put("lastName", account.getLastName());
        section.put("username", account.getUsername());
        section.put("email", account.getEmail());
        section.put("imageName", account.getImageName());
        section.put("roles", account.getRoles());
        section.put("createdAt", account.getCreatedAt());
        section.put("updatedAt", account.getUpdatedAt());
        return section;
    }

    private Map<String, Object> securitySection(UserAccount account) {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("twoFactorEnabled", account.isTwoFactorEnabled());
        section.put("loginActivity", loginEventRepository.findTop20ByUserIdOrderByCreatedAtDesc(account.getId()).stream()
                .map(event -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("ipAddress", event.getIpAddress());
                    row.put("userAgent", event.getUserAgent());
                    row.put("at", event.getCreatedAt());
                    return row;
                })
                .toList());
        return section;
    }

    private Object section(String name, Supplier<Object> supplier) {
        try {
            Object value = supplier.get();
            return value != null ? value : Map.of("unavailable", true);
        } catch (Exception ex) {
            log.warn("Data export section '{}' unavailable: {}", name, ex.getMessage());
            return Map.of("unavailable", true);
        }
    }
}
