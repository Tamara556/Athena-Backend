package com.athena.user.controller;

import com.athena.common.security.AuthHeaders;
import com.athena.user.dto.SettingsResponse;
import com.athena.user.dto.UpdateSettingsRequest;
import com.athena.user.service.UserSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users/me/settings")
@RequiredArgsConstructor
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    @GetMapping
    public ResponseEntity<SettingsResponse> get(@RequestHeader(AuthHeaders.USER_ID) UUID userId) {
        return ResponseEntity.ok(userSettingsService.getForUser(userId));
    }

    @PutMapping
    public ResponseEntity<SettingsResponse> update(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                   @Valid @RequestBody UpdateSettingsRequest request) {
        return ResponseEntity.ok(userSettingsService.update(userId, request));
    }
}
