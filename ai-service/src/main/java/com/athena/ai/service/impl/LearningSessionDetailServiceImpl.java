package com.athena.ai.service.impl;

import com.athena.ai.constants.AiConstants;
import com.athena.ai.dto.LearningSessionResponse;
import com.athena.ai.dto.PracticeActivityResponse;
import com.athena.ai.dto.QuizQuestionResponse;
import com.athena.ai.dto.ReadingMaterialResponse;
import com.athena.ai.dto.WatchingMaterialResponse;
import com.athena.ai.entity.LearningSession;
import com.athena.ai.repository.LearningSessionRepository;
import com.athena.ai.repository.PracticeActivityRepository;
import com.athena.ai.repository.QuizQuestionRepository;
import com.athena.ai.repository.ReadingMaterialRepository;
import com.athena.ai.repository.WatchingMaterialRepository;
import com.athena.ai.service.LearningSessionDetailService;
import com.athena.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearningSessionDetailServiceImpl implements LearningSessionDetailService {

    private final LearningSessionRepository sessionRepository;
    private final ReadingMaterialRepository readingRepository;
    private final WatchingMaterialRepository watchingRepository;
    private final PracticeActivityRepository practiceRepository;
    private final QuizQuestionRepository quizRepository;

    @Override
    @Cacheable(value = AiConstants.CACHE_LEARNING_SESSION, key = "#sessionId")
    @Transactional(readOnly = true)
    public LearningSessionResponse getDetail(UUID sessionId) {
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ResourceNotFoundException.of(AiConstants.RESOURCE_LEARNING_SESSION, sessionId));

        var readings = readingRepository.findBySessionIdOrderByOrderIndexAsc(sessionId).stream()
                .map(r -> new ReadingMaterialResponse(r.getId(), r.getTitle(), r.getContent(),
                        r.getEstimatedMinutes(), r.getOrderIndex()))
                .toList();
        var watchings = watchingRepository.findBySessionIdOrderByOrderIndexAsc(sessionId).stream()
                .map(w -> new WatchingMaterialResponse(w.getId(), w.getTitle(), w.getDescription(),
                        w.getVideoQuery(), w.getEstimatedMinutes(), w.getOrderIndex()))
                .toList();
        var practices = practiceRepository.findBySessionIdOrderByOrderIndexAsc(sessionId).stream()
                .map(p -> new PracticeActivityResponse(p.getId(), p.getTitle(), p.getDescription(),
                        p.getPracticeType().name(), p.getInstructions(), p.getStarterContent(),
                        p.getEstimatedMinutes(), p.getOrderIndex()))
                .toList();
        var quizzes = quizRepository.findBySessionIdOrderByOrderIndexAsc(sessionId).stream()
                .map(q -> new QuizQuestionResponse(q.getId(), q.getQuestion(), q.getType().name(),
                        q.getOptions(), q.getCorrectAnswer(), q.getExplanation(), q.getOrderIndex()))
                .toList();

        return new LearningSessionResponse(
                session.getId(), session.getRoadmapId(), session.getRoadmapNodeId(), session.getNodeIndex(),
                session.getTitle(), session.getStatus().name(), session.getEstimatedMinutes(),
                session.getGeneratedAt(), session.getUpdatedAt(), readings, watchings, practices, quizzes);
    }

    @Override
    @CacheEvict(value = AiConstants.CACHE_LEARNING_SESSION, key = "#sessionId")
    public void evict(UUID sessionId) {
    }
}
