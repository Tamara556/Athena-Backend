package com.athena.auth.service;

import com.athena.auth.dto.AccountResponse;
import com.athena.auth.dto.ChangeEmailRequest;
import com.athena.auth.dto.ChangePasswordRequest;
import com.athena.auth.dto.LoginActivityResponse;
import com.athena.auth.dto.TwoFactorSetupResponse;
import com.athena.auth.dto.TwoFactorStatusResponse;
import com.athena.auth.dto.UpdateProfileRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface AccountService {

    AccountResponse getAccount(UUID userId);

    List<LoginActivityResponse> getLoginActivity(UUID userId);

    TwoFactorStatusResponse getTwoFactorStatus(UUID userId);

    TwoFactorSetupResponse setupTwoFactor(UUID userId, String phoneNumber);

    TwoFactorStatusResponse enableTwoFactor(UUID userId, String code);

    TwoFactorSetupResponse sendDisableCode(UUID userId);

    TwoFactorStatusResponse disableTwoFactor(UUID userId, String code);

    AccountResponse updateProfile(UUID userId, UpdateProfileRequest request);

    AccountResponse changeEmail(UUID userId, ChangeEmailRequest request);

    void changePassword(UUID userId, ChangePasswordRequest request);

    AccountResponse uploadImage(UUID userId, MultipartFile image);
}
