package com.athena.progress.repository;

import com.athena.progress.entity.LearningProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LearningProgressRepository extends JpaRepository<LearningProgress, UUID> {
}
