package com.athena.learning.service.impl;

import com.athena.common.exception.ResourceNotFoundException;
import com.athena.learning.constants.LearningConstants;
import com.athena.learning.domain.TaskStatus;
import com.athena.learning.dto.EndSessionRequest;
import com.athena.learning.dto.SessionResponse;
import com.athena.learning.dto.StartSessionRequest;
import com.athena.learning.entity.LearningSession;
import com.athena.learning.entity.LearningTask;
import com.athena.learning.mapper.LearningMapper;
import com.athena.learning.repository.LearningSessionRepository;
import com.athena.learning.repository.LearningTaskRepository;
import com.athena.learning.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final LearningSessionRepository sessionRepository;
    private final LearningTaskRepository taskRepository;
    private final Clock clock;

    @Override
    @Transactional
    public SessionResponse start(UUID userId, StartSessionRequest request) {
        LearningTask task = taskRepository.findById(request.taskId())
                .orElseThrow(() -> ResourceNotFoundException.of(LearningConstants.LEARNING_TASK_RESOURCE, request.taskId()));

        if (task.getStatus() == TaskStatus.PENDING) {
            task.setStatus(TaskStatus.IN_PROGRESS);
            taskRepository.save(task);
        }

        LearningSession session = new LearningSession(userId, request.taskId(), clock.instant());
        LearningSession saved = sessionRepository.save(session);
        log.info("Started session sessionId={} taskId={} userId={}", saved.getId(), request.taskId(), userId);
        return LearningMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public SessionResponse end(EndSessionRequest request) {
        LearningSession session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> ResourceNotFoundException.of(LearningConstants.LEARNING_SESSION_RESOURCE, request.sessionId()));

        if (!session.isEnded()) {
            session.end(clock.instant());
            sessionRepository.save(session);
            log.info("Ended session sessionId={} durationMinutes={}", session.getId(), session.getDurationMinutes());
        }
        return LearningMapper.toResponse(session);
    }
}
