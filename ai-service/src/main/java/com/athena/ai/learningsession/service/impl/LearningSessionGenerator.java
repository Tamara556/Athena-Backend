package com.athena.ai.learningsession.service.impl;

import com.athena.ai.learningsession.domain.PracticeType;
import com.athena.ai.learningsession.domain.QuizType;
import com.athena.ai.roadmap.entity.GeneratedRoadmap;
import com.athena.ai.learningsession.entity.LearningSession;
import com.athena.ai.learningsession.entity.PracticeActivity;
import com.athena.ai.learningsession.entity.QuizQuestion;
import com.athena.ai.learningsession.entity.ReadingMaterial;
import com.athena.ai.learningsession.entity.WatchingMaterial;
import com.athena.ai.generation.messaging.AiEventPublisher;
import com.athena.ai.generation.model.LearningSessionContent;
import com.athena.ai.generation.model.RoadmapContent;
import com.athena.ai.learningsession.repository.LearningSessionRepository;
import com.athena.ai.learningsession.repository.PracticeActivityRepository;
import com.athena.ai.learningsession.repository.QuizQuestionRepository;
import com.athena.ai.learningsession.repository.ReadingMaterialRepository;
import com.athena.ai.learningsession.repository.WatchingMaterialRepository;
import com.athena.ai.generation.service.AiGenerationService;
import com.athena.common.event.KafkaTopics;
import com.athena.common.event.LearningSessionGeneratedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningSessionGenerator {

    private final LearningSessionRepository sessionRepository;
    private final ReadingMaterialRepository readingRepository;
    private final WatchingMaterialRepository watchingRepository;
    private final PracticeActivityRepository practiceRepository;
    private final QuizQuestionRepository quizRepository;
    private final AiGenerationService generation;
    private final AiEventPublisher events;
    private final Clock clock;

    public static UUID nodeId(UUID roadmapId, int nodeIndex) {
        return UUID.nameUUIDFromBytes((roadmapId + ":" + nodeIndex).getBytes(StandardCharsets.UTF_8));
    }

    @Transactional
    public LearningSession generate(UUID userId, GeneratedRoadmap roadmap, RoadmapContent content,
                                    int nodeIndex, String domain) {
        UUID roadmapNodeId = nodeId(roadmap.getId(), nodeIndex);
        Optional<LearningSession> existing = sessionRepository.findByUserIdAndRoadmapNodeId(userId, roadmapNodeId);
        if (existing.isPresent()) {
            return existing.get();
        }

        RoadmapContent.Phase phase = content.phases().get(nodeIndex);
        LearningSessionContent generated = generation.generateLearningSession(
                userId, roadmap.getGoal(), domain, level(roadmap.getLevel()), phase.name(), objectives(phase));

        LearningSession session = sessionRepository.save(new LearningSession(
                userId, roadmap.getId(), roadmapNodeId, nodeIndex, phase.name(), totalMinutes(generated)));
        persistStages(session.getId(), generated);

        events.publish(KafkaTopics.LEARNING_SESSION_GENERATED, userId,
                new LearningSessionGeneratedEvent(userId, session.getId(), roadmap.getId(), nodeIndex,
                        Instant.now(clock)));
        log.info("Generated learning session userId={} nodeIndex={} sessionId={}", userId, nodeIndex, session.getId());
        return session;
    }

    private void persistStages(UUID sessionId, LearningSessionContent content) {
        List<LearningSessionContent.Reading> readings = nullSafe(content.readings());
        for (int i = 0; i < readings.size(); i++) {
            LearningSessionContent.Reading r = readings.get(i);
            readingRepository.save(new ReadingMaterial(sessionId, r.title(), r.content(), r.estimatedMinutes(), i));
        }
        List<LearningSessionContent.Watching> watchings = nullSafe(content.watchings());
        for (int i = 0; i < watchings.size(); i++) {
            LearningSessionContent.Watching w = watchings.get(i);
            watchingRepository.save(new WatchingMaterial(
                    sessionId, w.title(), w.description(), w.videoQuery(), w.videoId(), w.estimatedMinutes(), i));
        }
        List<LearningSessionContent.Practice> practices = nullSafe(content.practices());
        for (int i = 0; i < practices.size(); i++) {
            LearningSessionContent.Practice p = practices.get(i);
            practiceRepository.save(new PracticeActivity(
                    sessionId, p.title(), p.description(), PracticeType.fromString(p.practiceType()),
                    p.instructions(), p.starterContent(), p.estimatedMinutes(), i));
        }
        List<LearningSessionContent.Quiz> quizzes = nullSafe(content.quizzes());
        for (int i = 0; i < quizzes.size(); i++) {
            LearningSessionContent.Quiz q = quizzes.get(i);
            quizRepository.save(new QuizQuestion(
                    sessionId, q.question(), QuizType.fromString(q.type()), nullSafe(q.options()),
                    q.correctAnswer(), q.explanation(), i));
        }
    }

    private int totalMinutes(LearningSessionContent content) {
        int total = 0;
        for (LearningSessionContent.Reading r : nullSafe(content.readings())) {
            total += r.estimatedMinutes();
        }
        for (LearningSessionContent.Watching w : nullSafe(content.watchings())) {
            total += w.estimatedMinutes();
        }
        for (LearningSessionContent.Practice p : nullSafe(content.practices())) {
            total += p.estimatedMinutes();
        }
        return total;
    }

    private String objectives(RoadmapContent.Phase phase) {
        List<String> objectives = nullSafe(phase.objectives());
        StringBuilder sb = new StringBuilder();
        for (String objective : objectives) {
            sb.append("- ").append(objective).append('\n');
        }
        return sb.isEmpty() ? "(none provided)" : sb.toString();
    }

    private String level(String level) {
        return level == null || level.isBlank() ? "beginner" : level;
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }
}
