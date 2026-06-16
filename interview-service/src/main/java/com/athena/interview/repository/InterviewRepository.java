package com.athena.interview.repository;

import com.athena.interview.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterviewRepository extends JpaRepository<Interview, UUID> {

    List<Interview> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
