package com.athena.ai.controller;

import com.athena.ai.dto.AssessmentResponse;
import com.athena.ai.dto.OnboardingResultResponse;
import com.athena.ai.dto.OnboardingStateResponse;
import com.athena.ai.dto.StartOnboardingResponse;
import com.athena.ai.dto.SubmitAssessmentRequest;
import com.athena.ai.dto.SubmitGoalRequest;
import com.athena.ai.service.OnboardingService;
import com.athena.common.security.AuthHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/ai/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @PostMapping("/start")
    public ResponseEntity<StartOnboardingResponse> start(@RequestHeader(AuthHeaders.USER_ID) UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(onboardingService.start(userId));
    }

    @PostMapping("/goal")
    public ResponseEntity<AssessmentResponse> submitGoal(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                         @Valid @RequestBody SubmitGoalRequest request) {
        return ResponseEntity.ok(onboardingService.submitGoal(userId, request));
    }

    @PostMapping("/assessment")
    public ResponseEntity<OnboardingResultResponse> submitAssessment(
            @RequestHeader(AuthHeaders.USER_ID) UUID userId,
            @Valid @RequestBody SubmitAssessmentRequest request) {
        return ResponseEntity.ok(onboardingService.submitAssessment(userId, request));
    }

    @GetMapping("/me")
    public ResponseEntity<OnboardingStateResponse> myState(@RequestHeader(AuthHeaders.USER_ID) UUID userId) {
        return ResponseEntity.ok(onboardingService.getState(userId));
    }
}
