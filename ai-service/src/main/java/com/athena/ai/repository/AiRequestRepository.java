package com.athena.ai.repository;

import com.athena.ai.entity.AiRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiRequestRepository extends JpaRepository<AiRequest, UUID> {
}
