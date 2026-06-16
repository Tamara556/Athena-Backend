package com.athena.ai.repository;

import com.athena.ai.entity.OnboardingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OnboardingSessionRepository extends JpaRepository<OnboardingSession, UUID> {

    Optional<OnboardingSession> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
}
