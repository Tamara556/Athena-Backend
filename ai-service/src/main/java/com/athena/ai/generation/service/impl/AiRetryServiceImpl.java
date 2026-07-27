package com.athena.ai.generation.service.impl;

import com.athena.ai.constants.AiConstants;
import com.athena.ai.generation.domain.RetryStatus;
import com.athena.ai.generation.entity.AiRequestRetry;
import com.athena.ai.generation.repository.AiRequestRetryRepository;
import com.athena.ai.generation.service.AiRetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiRetryServiceImpl implements AiRetryService {

    private final AiRequestRetryRepository repository;

    @Override
    @Transactional
    public AiRequestRetry record(String requestType, UUID userId, String payloadReference) {
        return repository.save(new AiRequestRetry(requestType, userId, payloadReference));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiRequestRetry> findRetryable() {
        return repository.findByStatusAndRetryCountLessThan(RetryStatus.PENDING, AiConstants.MAX_RETRIES);
    }

    @Override
    @Transactional
    public void markProcessing(AiRequestRetry retry) {
        retry.setStatus(RetryStatus.PROCESSING);
        repository.save(retry);
    }

    @Override
    @Transactional
    public void markCompleted(AiRequestRetry retry) {
        retry.setStatus(RetryStatus.COMPLETED);
        repository.save(retry);
    }

    @Override
    @Transactional
    public void markRetryableFailure(AiRequestRetry retry) {
        retry.setRetryCount(retry.getRetryCount() + 1);
        retry.setStatus(retry.getRetryCount() >= AiConstants.MAX_RETRIES ? RetryStatus.FAILED : RetryStatus.PENDING);
        repository.save(retry);
    }
}
