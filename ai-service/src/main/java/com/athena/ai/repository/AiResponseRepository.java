package com.athena.ai.repository;

import com.athena.ai.entity.AiResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiResponseRepository extends JpaRepository<AiResponse, UUID> {
}
