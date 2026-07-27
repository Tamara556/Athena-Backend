package com.athena.ai.generation.service.impl;

import com.athena.ai.client.AiException;
import com.athena.ai.client.AiTemporarilyUnavailableException;
import com.athena.ai.constants.AiConstants;
import com.athena.ai.onboarding.dto.AssessmentAnswer;
import com.athena.ai.generation.dto.RetryOutcomeResponse;
import com.athena.ai.generation.entity.AiRequestRetry;
import com.athena.ai.onboarding.entity.OnboardingSession;
import com.athena.ai.generation.repository.AiRequestRetryRepository;
import com.athena.ai.onboarding.repository.AssessmentRepository;
import com.athena.ai.onboarding.repository.OnboardingSessionRepository;
import com.athena.ai.generation.service.AiRetryService;
import com.athena.ai.onboarding.service.OnboardingService;
import com.athena.ai.generation.service.RetryDispatcher;
import com.athena.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetryDispatcherImpl implements RetryDispatcher {

    private final AiRequestRetryRepository retryRepository;
    private final AiRetryService retryService;
    private final OnboardingService onboardingService;
    private final OnboardingSessionRepository sessionRepository;
    private final AssessmentRepository assessmentRepository;
    private final ObjectMapper objectMapper;

    @Override
    public RetryOutcomeResponse retryNow(UUID requestId) {
        AiRequestRetry retry = retryRepository.findById(requestId)
                .orElseThrow(() -> ResourceNotFoundException.of(AiConstants.RESOURCE_RETRY, requestId));
        try {
            dispatch(retry);
            return new RetryOutcomeResponse(retry.getId(), "COMPLETED", "Your request has been processed.");
        } catch (AiException ex) {
            retryService.markRetryableFailure(retry);
            throw new AiTemporarilyUnavailableException(retry.getId());
        }
    }

    @Override
    @Scheduled(fixedDelayString = "${athena.ai.retry-scan-ms:60000}")
    public void retryPending() {
        List<AiRequestRetry> pending = retryService.findRetryable();
        if (pending.isEmpty()) {
            return;
        }
        log.info("Retrying {} pending AI request(s)", pending.size());
        for (AiRequestRetry retry : pending) {
            try {
                dispatch(retry);
            } catch (AiException ex) {
                retryService.markRetryableFailure(retry);
                log.warn("Retry still failing id={} attempt={}", retry.getId(), retry.getRetryCount());
            }
        }
    }

    private void dispatch(AiRequestRetry retry) {
        retryService.markProcessing(retry);
        OnboardingSession session = sessionRepository.findById(UUID.fromString(retry.getPayloadReference()))
                .orElseThrow(() -> ResourceNotFoundException.of(AiConstants.RESOURCE_ONBOARDING,
                        retry.getPayloadReference()));

        switch (retry.getRequestType()) {
            case AiConstants.RETRY_ASSESSMENT -> onboardingService.generateAssessmentForSession(session);
            case AiConstants.RETRY_COMPLETE ->
                    onboardingService.completeOnboardingForSession(session, loadAnswers(session.getId()));
            default -> log.warn("Unknown retry requestType={}", retry.getRequestType());
        }
        retryService.markCompleted(retry);
        log.info("Retry completed id={} type={}", retry.getId(), retry.getRequestType());
    }

    private List<AssessmentAnswer> loadAnswers(UUID sessionId) {
        return assessmentRepository.findFirstBySessionIdOrderByCreatedAtDesc(sessionId)
                .map(a -> a.getAnswersJson() == null ? new AssessmentAnswer[0]
                        : objectMapper.readValue(a.getAnswersJson(), AssessmentAnswer[].class))
                .map(arr -> List.of(arr))
                .orElse(List.of());
    }
}
