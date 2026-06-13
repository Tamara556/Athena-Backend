package com.athena.user.service.impl;

import com.athena.common.exception.DuplicateResourceException;
import com.athena.common.exception.ResourceNotFoundException;
import com.athena.user.constants.UserConstants;
import com.athena.user.dto.CreateUserProfileRequest;
import com.athena.user.dto.UpdateUserProfileRequest;
import com.athena.user.dto.UserProfileResponse;
import com.athena.user.entity.UserProfile;
import com.athena.user.mapper.UserProfileMapper;
import com.athena.user.repository.UserProfileRepository;
import com.athena.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository repository;

    @Override
    @Transactional
    public UserProfileResponse create(CreateUserProfileRequest request) {
        if (repository.existsById(request.userId())) {
            throw new DuplicateResourceException(UserConstants.PROFILE_ALREADY_EXISTS.formatted(request.userId()));
        }
        UserProfile saved = repository.save(UserProfileMapper.toEntity(request));
        log.info("Created user profile userId={}", saved.getUserId());
        return UserProfileMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getById(UUID userId) {
        return repository.findById(userId)
                .map(UserProfileMapper::toResponse)
                .orElseThrow(() -> ResourceNotFoundException.of(UserConstants.RESOURCE_NAME, userId));
    }

    @Override
    @Transactional
    public UserProfileResponse update(UUID userId, UpdateUserProfileRequest request) {
        UserProfile profile = repository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of(UserConstants.RESOURCE_NAME, userId));
        UserProfileMapper.applyUpdate(profile, request);
        UserProfile saved = repository.save(profile);
        log.info("Updated user profile userId={}", userId);
        return UserProfileMapper.toResponse(saved);
    }
}
