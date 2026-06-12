package com.athena.user.service;

import com.athena.user.dto.CreateUserProfileRequest;
import com.athena.user.dto.UpdateUserProfileRequest;
import com.athena.user.dto.UserProfileResponse;

import java.util.UUID;

public interface UserProfileService {

    UserProfileResponse create(CreateUserProfileRequest request);

    UserProfileResponse getById(UUID userId);

    UserProfileResponse update(UUID userId, UpdateUserProfileRequest request);
}
