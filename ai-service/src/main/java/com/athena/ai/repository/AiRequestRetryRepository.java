package com.athena.ai.repository;

import com.athena.ai.domain.RetryStatus;
import com.athena.ai.entity.AiRequestRetry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiRequestRetryRepository extends JpaRepository<AiRequestRetry, UUID> {

    List<AiRequestRetry> findByStatusAndRetryCountLessThan(RetryStatus status, int maxRetries);
}
