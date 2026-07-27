package com.athena.auth.controller;

import com.athena.auth.dto.AccountResponse;
import com.athena.auth.dto.ChangeEmailRequest;
import com.athena.auth.dto.ChangePasswordRequest;
import com.athena.auth.dto.DeviceResponse;
import com.athena.auth.dto.LoginActivityResponse;
import com.athena.auth.dto.TwoFactorCodeRequest;
import com.athena.auth.dto.TwoFactorSetupRequest;
import com.athena.auth.dto.TwoFactorSetupResponse;
import com.athena.auth.dto.TwoFactorStatusResponse;
import com.athena.auth.dto.UpdateProfileRequest;
import com.athena.auth.service.AccountService;
import com.athena.auth.service.DataExportService;
import com.athena.auth.service.DeviceService;
import com.athena.common.security.AuthHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final DataExportService dataExportService;
    private final DeviceService deviceService;

    @GetMapping("/me")
    public ResponseEntity<AccountResponse> me(@RequestHeader(AuthHeaders.USER_ID) UUID userId) {
        return ResponseEntity.ok(accountService.getAccount(userId));
    }

    @GetMapping("/devices")
    public ResponseEntity<List<DeviceResponse>> devices(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                        @RequestParam(value = "current", required = false) UUID current) {
        return ResponseEntity.ok(deviceService.list(userId, current));
    }

    @PostMapping("/devices/{id}/revoke")
    public ResponseEntity<Void> revokeDevice(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                             @PathVariable("id") UUID id) {
        deviceService.revoke(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/devices/revoke-others")
    public ResponseEntity<Void> revokeOtherDevices(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                   @RequestParam(value = "current", required = false) UUID current) {
        deviceService.revokeOthers(userId, current);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> export(@RequestHeader(AuthHeaders.USER_ID) UUID userId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"athena-data-export.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dataExportService.export(userId));
    }

    @GetMapping("/login-activity")
    public ResponseEntity<List<LoginActivityResponse>> loginActivity(@RequestHeader(AuthHeaders.USER_ID) UUID userId) {
        return ResponseEntity.ok(accountService.getLoginActivity(userId));
    }

    @GetMapping("/2fa")
    public ResponseEntity<TwoFactorStatusResponse> twoFactorStatus(@RequestHeader(AuthHeaders.USER_ID) UUID userId) {
        return ResponseEntity.ok(accountService.getTwoFactorStatus(userId));
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<TwoFactorSetupResponse> setupTwoFactor(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                                 @Valid @RequestBody TwoFactorSetupRequest request) {
        return ResponseEntity.ok(accountService.setupTwoFactor(userId, request.phoneNumber()));
    }

    @PostMapping("/2fa/enable")
    public ResponseEntity<TwoFactorStatusResponse> enableTwoFactor(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                                   @Valid @RequestBody TwoFactorCodeRequest request) {
        return ResponseEntity.ok(accountService.enableTwoFactor(userId, request.code()));
    }

    @PostMapping("/2fa/send-code")
    public ResponseEntity<TwoFactorSetupResponse> sendDisableCode(@RequestHeader(AuthHeaders.USER_ID) UUID userId) {
        return ResponseEntity.ok(accountService.sendDisableCode(userId));
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<TwoFactorStatusResponse> disableTwoFactor(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                                    @Valid @RequestBody TwoFactorCodeRequest request) {
        return ResponseEntity.ok(accountService.disableTwoFactor(userId, request.code()));
    }

    @PatchMapping("/profile")
    public ResponseEntity<AccountResponse> updateProfile(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                         @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(accountService.updateProfile(userId, request));
    }

    @PostMapping("/email")
    public ResponseEntity<AccountResponse> changeEmail(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                       @Valid @RequestBody ChangeEmailRequest request) {
        return ResponseEntity.ok(accountService.changeEmail(userId, request));
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        accountService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AccountResponse> uploadImage(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                       @RequestPart("image") MultipartFile image) {
        return ResponseEntity.ok(accountService.uploadImage(userId, image));
    }
}
