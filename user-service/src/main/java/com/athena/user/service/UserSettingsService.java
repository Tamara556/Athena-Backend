package com.athena.user.service;

import com.athena.user.dto.SettingsResponse;
import com.athena.user.dto.UpdateSettingsRequest;

import java.util.UUID;

public interface UserSettingsService {

    SettingsResponse getForUser(UUID userId);

    SettingsResponse update(UUID userId, UpdateSettingsRequest request);
}
