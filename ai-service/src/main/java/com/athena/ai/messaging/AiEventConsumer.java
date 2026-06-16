package com.athena.ai.messaging;

import com.athena.ai.service.KnowledgeGraphService;
import com.athena.ai.service.OnboardingService;
import com.athena.common.event.InterviewEvaluatedEvent;
import com.athena.common.event.KafkaTopics;
import com.athena.common.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiEventConsumer {

    private final OnboardingService onboardingService;
    private final KnowledgeGraphService knowledgeGraph;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.USER_REGISTERED, groupId = "${spring.kafka.consumer.group-id}")
    public void onUserRegistered(String payload) {
        UserRegisteredEvent event = objectMapper.readValue(payload, UserRegisteredEvent.class);
        log.info("Received UserRegisteredEvent userId={}", event.userId());
        onboardingService.createSessionFromRegistration(event.userId(), event.firstName(), event.lastName());
    }

    @KafkaListener(topics = KafkaTopics.INTERVIEW_EVALUATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onInterviewEvaluated(String payload) {
        InterviewEvaluatedEvent event = objectMapper.readValue(payload, InterviewEvaluatedEvent.class);
        log.info("Received InterviewEvaluatedEvent userId={} score={}", event.userId(), event.score());
        knowledgeGraph.recordWeaknesses(event.userId(), event.domain(), event.weaknesses());
    }
}
