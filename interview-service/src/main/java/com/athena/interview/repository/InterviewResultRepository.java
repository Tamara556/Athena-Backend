package com.athena.interview.repository;

import com.athena.interview.entity.InterviewResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InterviewResultRepository extends JpaRepository<InterviewResult, UUID> {

    Optional<InterviewResult> findByInterviewId(UUID interviewId);
}
