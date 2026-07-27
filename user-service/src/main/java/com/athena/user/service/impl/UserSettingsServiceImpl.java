package com.athena.user.service.impl;

import com.athena.user.dto.SettingsResponse;
import com.athena.user.dto.UpdateSettingsRequest;
import com.athena.user.entity.UserSettings;
import com.athena.user.repository.UserSettingsRepository;
import com.athena.user.service.UserSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSettingsServiceImpl implements UserSettingsService {

    private final UserSettingsRepository repository;

    @Override
    @Transactional
    public SettingsResponse getForUser(UUID userId) {
        UserSettings settings = repository.findById(userId)
                .orElseGet(() -> repository.save(new UserSettings(userId)));
        return toResponse(settings);
    }

    @Override
    @Transactional
    public SettingsResponse update(UUID userId, UpdateSettingsRequest request) {
        UserSettings settings = repository.findById(userId).orElseGet(() -> new UserSettings(userId));
        apply(settings, request);
        UserSettings saved = repository.save(settings);
        log.info("Updated user settings userId={}", userId);
        return toResponse(saved);
    }

    private void apply(UserSettings settings, UpdateSettingsRequest request) {
        settings.setAvailability(request.learning().availability());
        settings.setDifficulty(request.learning().difficulty());
        settings.setStyle(request.learning().style());
        settings.setTone(request.experience().tone());
        settings.setMotivational(request.experience().motivational());
        settings.setReflection(request.experience().reflection());
        settings.setAdaptive(request.experience().adaptive());
        settings.setDailyReminder(request.notifications().dailyReminder());
        settings.setWeeklySummary(request.notifications().weeklySummary());
        settings.setInterviewReminders(request.notifications().interviewReminders());
        settings.setMilestones(request.notifications().milestones());
        settings.setPersonalize(request.privacy().personalize());
        settings.setShareAnon(request.privacy().shareAnon());
    }

    private SettingsResponse toResponse(UserSettings settings) {
        return new SettingsResponse(
                new SettingsResponse.Learning(settings.getAvailability(), settings.getDifficulty(), settings.getStyle()),
                new SettingsResponse.Experience(settings.getTone(), settings.isMotivational(), settings.isReflection(),
                        settings.isAdaptive()),
                new SettingsResponse.Notifications(settings.isDailyReminder(), settings.isWeeklySummary(),
                        settings.isInterviewReminders(), settings.isMilestones()),
                new SettingsResponse.Privacy(settings.isPersonalize(), settings.isShareAnon()));
    }
}
