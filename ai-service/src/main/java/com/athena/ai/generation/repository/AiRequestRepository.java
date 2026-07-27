package com.athena.ai.generation.repository;

import com.athena.ai.generation.entity.AiRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiRequestRepository extends JpaRepository<AiRequest, UUID> {
}
