package com.athena.ai.onboarding.service;

import com.athena.ai.onboarding.dto.AssessmentAnswer;
import com.athena.ai.onboarding.dto.AssessmentResponse;
import com.athena.ai.onboarding.dto.OnboardingResultResponse;
import com.athena.ai.onboarding.dto.OnboardingStateResponse;
import com.athena.ai.onboarding.dto.StartOnboardingResponse;
import com.athena.ai.onboarding.dto.SubmitAssessmentRequest;
import com.athena.ai.onboarding.dto.SubmitGoalRequest;
import com.athena.ai.onboarding.entity.OnboardingSession;

import java.util.List;
import java.util.UUID;

public interface OnboardingService {

    OnboardingSession createSessionFromRegistration(UUID userId, String firstName, String lastName);

    @Deprecated
    StartOnboardingResponse start(UUID userId);

    AssessmentResponse submitGoal(UUID userId, SubmitGoalRequest request);

    OnboardingResultResponse submitAssessment(UUID userId, SubmitAssessmentRequest request);

    OnboardingStateResponse getState(UUID userId);

    AssessmentResponse generateAssessmentForSession(OnboardingSession session);

    OnboardingResultResponse completeOnboardingForSession(OnboardingSession session,
                                                          List<AssessmentAnswer> answers);
}
