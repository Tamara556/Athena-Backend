package com.athena.ai.service;

import com.athena.ai.dto.AssessmentAnswer;
import com.athena.ai.dto.AssessmentResponse;
import com.athena.ai.dto.OnboardingResultResponse;
import com.athena.ai.dto.OnboardingStateResponse;
import com.athena.ai.dto.StartOnboardingResponse;
import com.athena.ai.dto.SubmitAssessmentRequest;
import com.athena.ai.dto.SubmitGoalRequest;
import com.athena.ai.entity.OnboardingSession;

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
