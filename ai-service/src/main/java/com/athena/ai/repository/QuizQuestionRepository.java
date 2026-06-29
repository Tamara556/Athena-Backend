package com.athena.ai.repository;

import com.athena.ai.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {

    List<QuizQuestion> findBySessionIdOrderByOrderIndexAsc(UUID sessionId);
}
