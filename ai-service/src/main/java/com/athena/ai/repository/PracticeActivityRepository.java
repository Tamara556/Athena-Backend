package com.athena.ai.repository;

import com.athena.ai.entity.PracticeActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PracticeActivityRepository extends JpaRepository<PracticeActivity, UUID> {

    List<PracticeActivity> findBySessionIdOrderByOrderIndexAsc(UUID sessionId);
}
