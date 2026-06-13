package com.athena.learning.repository;

import com.athena.learning.entity.LearningSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LearningSessionRepository extends JpaRepository<LearningSession, UUID> {
}
