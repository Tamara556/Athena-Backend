package com.athena.learning.service;

import com.athena.common.event.TaskCompletedEvent;
import com.athena.common.exception.ResourceNotFoundException;
import com.athena.learning.domain.TaskStatus;
import com.athena.learning.domain.TaskType;
import com.athena.learning.dto.TaskResponse;
import com.athena.learning.entity.LearningPlan;
import com.athena.learning.entity.LearningTask;
import com.athena.learning.messaging.LearningEventPublisher;
import com.athena.learning.repository.LearningPlanRepository;
import com.athena.learning.repository.LearningTaskRepository;
import com.athena.learning.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private LearningTaskRepository taskRepository;
    @Mock
    private LearningPlanRepository planRepository;
    @Mock
    private LearningEventPublisher eventPublisher;

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-12T10:00:00Z"), ZoneOffset.UTC);

    private TaskServiceImpl service() {
        return new TaskServiceImpl(taskRepository, planRepository, eventPublisher, clock);
    }

    private final UUID taskId = UUID.randomUUID();
    private final UUID planId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private LearningTask pendingTask() {
        LearningTask task = new LearningTask();
        task.setId(taskId);
        task.setPlanId(planId);
        task.setTitle("Read chapter 1");
        task.setTaskType(TaskType.READING);
        task.setEstimatedMinutes(30);
        task.setStatus(TaskStatus.PENDING);
        return task;
    }

    private LearningPlan ownerPlan() {
        LearningPlan plan = new LearningPlan();
        plan.setId(planId);
        plan.setUserId(userId);
        return plan;
    }

    @Test
    void complete_marksCompletedAndPublishesEvent() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(pendingTask()));
        when(planRepository.findById(planId)).thenReturn(Optional.of(ownerPlan()));
        when(taskRepository.save(any(LearningTask.class))).thenAnswer(i -> i.getArgument(0));

        TaskResponse response = service().complete(taskId);

        assertThat(response.status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(response.completedAt()).isEqualTo(Instant.parse("2026-06-12T10:00:00Z"));

        ArgumentCaptor<TaskCompletedEvent> captor = ArgumentCaptor.forClass(TaskCompletedEvent.class);
        verify(eventPublisher).publishTaskCompleted(captor.capture());
        TaskCompletedEvent event = captor.getValue();
        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.taskId()).isEqualTo(taskId);
        assertThat(event.planId()).isEqualTo(planId);
        assertThat(event.taskType()).isEqualTo("READING");
        assertThat(event.durationMinutes()).isEqualTo(30);
    }

    @Test
    void complete_isIdempotent_whenAlreadyCompleted() {
        LearningTask task = pendingTask();
        task.setStatus(TaskStatus.COMPLETED);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        service().complete(taskId);

        verify(taskRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void complete_throwsWhenTaskMissing() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().complete(taskId))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(eventPublisher);
    }
}
