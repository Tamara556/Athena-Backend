package com.athena.user.mapper;

import com.athena.user.dto.CreateUserProfileRequest;
import com.athena.user.dto.UpdateUserProfileRequest;
import com.athena.user.dto.UserProfileResponse;
import com.athena.user.entity.UserProfile;

/**
 * Pure mapping between the {@link UserProfile} entity and its DTOs. Keeping this
 * isolated guarantees entities never leak past the service boundary.
 */
public final class UserProfileMapper {

    private UserProfileMapper() {
    }

    public static UserProfile toEntity(CreateUserProfileRequest request) {
        UserProfile profile = new UserProfile();
        profile.setUserId(request.userId());
        profile.setName(request.name());
        profile.setAge(request.age());
        profile.setGoal(request.goal());
        profile.setDailyStudyHours(request.dailyStudyHours());
        return profile;
    }

    public static void applyUpdate(UserProfile profile, UpdateUserProfileRequest request) {
        profile.setName(request.name());
        profile.setAge(request.age());
        profile.setGoal(request.goal());
        profile.setDailyStudyHours(request.dailyStudyHours());
    }

    public static UserProfileResponse toResponse(UserProfile profile) {
        return new UserProfileResponse(
                profile.getUserId(),
                profile.getName(),
                profile.getAge(),
                profile.getGoal(),
                profile.getDailyStudyHours(),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }
}
