package com.athena.ai.messaging;

import com.athena.ai.service.LearningSessionService;
import com.athena.common.event.KafkaTopics;
import com.athena.common.event.LearningSessionCompletedEvent;
import com.athena.common.event.RoadmapGeneratedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class LearningSessionEventConsumer {

    private final LearningSessionService learningSessionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.ROADMAP_GENERATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onRoadmapGenerated(String payload) {
        RoadmapGeneratedEvent event = objectMapper.readValue(payload, RoadmapGeneratedEvent.class);
        log.info("Received RoadmapGeneratedEvent userId={} roadmapId={}", event.userId(), event.roadmapId());
        learningSessionService.generateInitialBuffer(event.userId(), event.roadmapId());
    }

    @KafkaListener(topics = KafkaTopics.LEARNING_SESSION_COMPLETED, groupId = "${spring.kafka.consumer.group-id}")
    public void onLearningSessionCompleted(String payload) {
        LearningSessionCompletedEvent event = objectMapper.readValue(payload, LearningSessionCompletedEvent.class);
        log.info("Received LearningSessionCompletedEvent userId={} nodeIndex={}", event.userId(), event.nodeIndex());
        learningSessionService.refillBuffer(event.userId());
    }
}
