package com.athena.user.service;

import com.athena.common.exception.DuplicateResourceException;
import com.athena.common.exception.ResourceNotFoundException;
import com.athena.user.dto.CreateUserProfileRequest;
import com.athena.user.dto.UpdateUserProfileRequest;
import com.athena.user.dto.UserProfileResponse;
import com.athena.user.entity.UserProfile;
import com.athena.user.repository.UserProfileRepository;
import com.athena.user.service.impl.UserProfileServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private UserProfileRepository repository;

    @InjectMocks
    private UserProfileServiceImpl service;

    private final UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private UserProfile existingProfile() {
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setName("Ada");
        profile.setAge(30);
        profile.setGoal("Master algorithms");
        profile.setDailyStudyHours(2.5);
        return profile;
    }

    @Test
    void create_savesNewProfile() {
        CreateUserProfileRequest request =
                new CreateUserProfileRequest(userId, "Ada", 30, "Master algorithms", 2.5);
        when(repository.existsById(userId)).thenReturn(false);
        when(repository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        UserProfileResponse response = service.create(request);

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.name()).isEqualTo("Ada");
        assertThat(response.dailyStudyHours()).isEqualTo(2.5);
        verify(repository).save(any(UserProfile.class));
    }

    @Test
    void create_rejectsDuplicate() {
        when(repository.existsById(userId)).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new CreateUserProfileRequest(userId, "Ada", 30, "Goal", 2.0)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void getById_returnsProfile() {
        when(repository.findById(userId)).thenReturn(Optional.of(existingProfile()));

        UserProfileResponse response = service.getById(userId);

        assertThat(response.name()).isEqualTo("Ada");
    }

    @Test
    void getById_throwsWhenMissing() {
        when(repository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_appliesChanges() {
        when(repository.findById(userId)).thenReturn(Optional.of(existingProfile()));
        when(repository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        UserProfileResponse response = service.update(userId,
                new UpdateUserProfileRequest("Ada Lovelace", 31, "Ship Athena", 4.0));

        assertThat(response.name()).isEqualTo("Ada Lovelace");
        assertThat(response.age()).isEqualTo(31);
        assertThat(response.dailyStudyHours()).isEqualTo(4.0);
    }

    @Test
    void update_throwsWhenMissing() {
        when(repository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(userId,
                new UpdateUserProfileRequest("X", 20, "Goal", 1.0)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
