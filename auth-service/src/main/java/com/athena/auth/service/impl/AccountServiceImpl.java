package com.athena.auth.service.impl;

import com.athena.auth.constants.AuthConstants;
import com.athena.auth.dto.AccountResponse;
import com.athena.auth.dto.ChangeEmailRequest;
import com.athena.auth.dto.ChangePasswordRequest;
import com.athena.auth.dto.LoginActivityResponse;
import com.athena.auth.dto.TwoFactorSetupResponse;
import com.athena.auth.dto.TwoFactorStatusResponse;
import com.athena.auth.dto.UpdateProfileRequest;
import com.athena.auth.entity.UserAccount;
import com.athena.auth.repository.LoginEventRepository;
import com.athena.auth.repository.UserAccountRepository;
import com.athena.auth.service.AccountService;
import com.athena.auth.service.VerificationCodeService;
import com.athena.auth.service.UserImageService;
import com.athena.auth.sms.SmsSender;
import com.athena.common.exception.DuplicateResourceException;
import com.athena.common.exception.InvalidCredentialsException;
import com.athena.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final UserAccountRepository userAccountRepository;
    private final LoginEventRepository loginEventRepository;
    private final VerificationCodeService verificationCodeService;
    private final SmsSender smsSender;
    private final PasswordEncoder passwordEncoder;
    private final UserImageService userImageService;

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID userId) {
        return toResponse(require(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoginActivityResponse> getLoginActivity(UUID userId) {
        return loginEventRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(event -> new LoginActivityResponse(event.getId(), event.getIpAddress(), event.getUserAgent(), event.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TwoFactorStatusResponse getTwoFactorStatus(UUID userId) {
        UserAccount account = require(userId);
        return new TwoFactorStatusResponse(account.isTwoFactorEnabled(), mask(account.getPhoneNumber()));
    }

    @Override
    @Transactional
    public TwoFactorSetupResponse setupTwoFactor(UUID userId, String phoneNumber) {
        UserAccount account = require(userId);
        if (account.isTwoFactorEnabled()) {
            throw new IllegalArgumentException(AuthConstants.TWO_FACTOR_ALREADY_ENABLED);
        }
        String normalized = phoneNumber.trim();
        account.setTwoFactorPendingPhone(normalized);
        String code = issueCode(account);
        userAccountRepository.save(account);
        smsSender.send(normalized, AuthConstants.TWO_FACTOR_SMS_MESSAGE.formatted(code));
        log.info("Started two-factor setup userId={}", userId);
        return new TwoFactorSetupResponse(mask(normalized));
    }

    @Override
    @Transactional
    public TwoFactorStatusResponse enableTwoFactor(UUID userId, String code) {
        UserAccount account = require(userId);
        String pendingPhone = account.getTwoFactorPendingPhone();
        if (pendingPhone == null) {
            throw new IllegalArgumentException(AuthConstants.TWO_FACTOR_SETUP_REQUIRED);
        }
        verifyCode(account, code);
        account.setPhoneNumber(pendingPhone);
        account.setTwoFactorPendingPhone(null);
        account.setTwoFactorEnabled(true);
        clearCode(account);
        userAccountRepository.save(account);
        log.info("Enabled two-factor userId={}", userId);
        return new TwoFactorStatusResponse(true, mask(pendingPhone));
    }

    @Override
    @Transactional
    public TwoFactorSetupResponse sendDisableCode(UUID userId) {
        UserAccount account = require(userId);
        if (!account.isTwoFactorEnabled()) {
            throw new IllegalArgumentException(AuthConstants.TWO_FACTOR_NOT_ENABLED);
        }
        String code = issueCode(account);
        userAccountRepository.save(account);
        smsSender.send(account.getPhoneNumber(), AuthConstants.TWO_FACTOR_SMS_MESSAGE.formatted(code));
        log.info("Sent two-factor disable code userId={}", userId);
        return new TwoFactorSetupResponse(mask(account.getPhoneNumber()));
    }

    @Override
    @Transactional
    public TwoFactorStatusResponse disableTwoFactor(UUID userId, String code) {
        UserAccount account = require(userId);
        if (!account.isTwoFactorEnabled()) {
            throw new IllegalArgumentException(AuthConstants.TWO_FACTOR_NOT_ENABLED);
        }
        verifyCode(account, code);
        account.setTwoFactorEnabled(false);
        account.setPhoneNumber(null);
        account.setTwoFactorPendingPhone(null);
        clearCode(account);
        userAccountRepository.save(account);
        log.info("Disabled two-factor userId={}", userId);
        return new TwoFactorStatusResponse(false, null);
    }

    private String issueCode(UserAccount account) {
        String code = verificationCodeService.generateCode();
        account.setTwoFactorCodeHash(verificationCodeService.hash(code));
        account.setTwoFactorCodeExpiresAt(Instant.now().plus(VerificationCodeService.CODE_TTL));
        return code;
    }

    private void verifyCode(UserAccount account, String code) {
        Instant expiry = account.getTwoFactorCodeExpiresAt();
        if (account.getTwoFactorCodeHash() == null || expiry == null) {
            throw new IllegalArgumentException(AuthConstants.TWO_FACTOR_SETUP_REQUIRED);
        }
        if (expiry.isBefore(Instant.now())) {
            throw new IllegalArgumentException(AuthConstants.TWO_FACTOR_CODE_EXPIRED);
        }
        if (!verificationCodeService.matches(code, account.getTwoFactorCodeHash())) {
            throw new IllegalArgumentException(AuthConstants.TWO_FACTOR_CODE_INVALID);
        }
    }

    private void clearCode(UserAccount account) {
        account.setTwoFactorCodeHash(null);
        account.setTwoFactorCodeExpiresAt(null);
    }

    private String mask(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 2) {
            return phoneNumber;
        }
        String last = phoneNumber.substring(phoneNumber.length() - 2);
        return "•".repeat(Math.max(0, phoneNumber.length() - 2)) + last;
    }

    @Override
    @Transactional
    public AccountResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        UserAccount account = require(userId);
        account.setFirstName(request.firstName().trim());
        account.setLastName(request.lastName().trim());
        log.info("Updated profile userId={}", userId);
        return toResponse(userAccountRepository.save(account));
    }

    @Override
    @Transactional
    public AccountResponse changeEmail(UUID userId, ChangeEmailRequest request) {
        UserAccount account = require(userId);
        verifyPassword(request.currentPassword(), account);

        String email = request.newEmail().trim().toLowerCase();
        if (!email.equalsIgnoreCase(account.getEmail()) && userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException(AuthConstants.EMAIL_ALREADY_EXISTS);
        }
        account.setEmail(email);
        log.info("Changed email userId={}", userId);
        return toResponse(userAccountRepository.save(account));
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        UserAccount account = require(userId);
        verifyPassword(request.currentPassword(), account);
        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userAccountRepository.save(account);
        log.info("Changed password userId={}", userId);
    }

    @Override
    @Transactional
    public AccountResponse uploadImage(UUID userId, MultipartFile image) {
        UserAccount account = require(userId);
        String imageName = userImageService.store(userId, image);
        account.setImageName(imageName);
        log.info("Updated avatar userId={}", userId);
        return toResponse(userAccountRepository.save(account));
    }

    private UserAccount require(UUID userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of(AuthConstants.ACCOUNT_RESOURCE_NAME, userId));
    }

    private void verifyPassword(String rawPassword, UserAccount account) {
        if (!passwordEncoder.matches(rawPassword, account.getPasswordHash())) {
            throw new InvalidCredentialsException(AuthConstants.CURRENT_PASSWORD_INCORRECT);
        }
    }

    private AccountResponse toResponse(UserAccount account) {
        return new AccountResponse(account.getId(), account.getFirstName(), account.getLastName(),
                account.getUsername(), account.getEmail(), account.getImageName());
    }
}
