package com.athena.ai.controller;

import com.athena.ai.dto.DailyPlanResponse;
import com.athena.ai.service.DailyPlanService;
import com.athena.common.security.AuthHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/ai/daily-plans")
@RequiredArgsConstructor
public class DailyPlanController {

    private final DailyPlanService dailyPlanService;

    @GetMapping("/me")
    public ResponseEntity<DailyPlanResponse> myDailyPlan(@RequestHeader(AuthHeaders.USER_ID) UUID userId) {
        return ResponseEntity.ok(dailyPlanService.getLatestForUser(userId));
    }
}
