package com.athena.badge.controller;

import com.athena.badge.dto.BadgeResponse;
import com.athena.badge.dto.UserBadgeResponse;
import com.athena.badge.service.BadgeService;
import com.athena.common.security.AuthHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    @GetMapping("/badges")
    public ResponseEntity<List<BadgeResponse>> all() {
        return ResponseEntity.ok(badgeService.getAllBadges());
    }

    @GetMapping("/badges/me")
    public ResponseEntity<List<UserBadgeResponse>> mine(@RequestHeader(AuthHeaders.USER_ID) UUID userId) {
        return ResponseEntity.ok(badgeService.getUserBadges(userId));
    }

    @GetMapping("/users/{userId}/badges")
    public ResponseEntity<List<UserBadgeResponse>> forUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(badgeService.getUserBadges(userId));
    }
}
