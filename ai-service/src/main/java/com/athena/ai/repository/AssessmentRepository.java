package com.athena.ai.repository;

import com.athena.ai.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {

    Optional<Assessment> findFirstBySessionIdOrderByCreatedAtDesc(UUID sessionId);
}
