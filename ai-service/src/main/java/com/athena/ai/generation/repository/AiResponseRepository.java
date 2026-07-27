package com.athena.ai.generation.repository;

import com.athena.ai.generation.entity.AiResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiResponseRepository extends JpaRepository<AiResponse, UUID> {
}
