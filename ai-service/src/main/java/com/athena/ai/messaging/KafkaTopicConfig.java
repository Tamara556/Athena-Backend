package com.athena.ai.messaging;

import com.athena.common.event.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    private NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic onboardingStartedTopic() {
        return topic(KafkaTopics.ONBOARDING_STARTED);
    }

    @Bean
    NewTopic goalDiscoveredTopic() {
        return topic(KafkaTopics.GOAL_DISCOVERED);
    }

    @Bean
    NewTopic assessmentCompletedTopic() {
        return topic(KafkaTopics.ASSESSMENT_COMPLETED);
    }

    @Bean
    NewTopic goalAnalyzedTopic() {
        return topic(KafkaTopics.GOAL_ANALYZED);
    }

    @Bean
    NewTopic roadmapGeneratedTopic() {
        return topic(KafkaTopics.ROADMAP_GENERATED);
    }

    @Bean
    NewTopic dailyPlanGeneratedTopic() {
        return topic(KafkaTopics.DAILY_PLAN_GENERATED);
    }

    @Bean
    NewTopic knowledgeGraphUpdatedTopic() {
        return topic(KafkaTopics.KNOWLEDGE_GRAPH_UPDATED);
    }

    @Bean
    NewTopic badgeSuggestionGeneratedTopic() {
        return topic(KafkaTopics.BADGE_SUGGESTION_GENERATED);
    }

    @Bean
    NewTopic knowledgeVisualizationGeneratedTopic() {
        return topic(KafkaTopics.KNOWLEDGE_GRAPH_VISUALIZATION_GENERATED);
    }

    @Bean
    NewTopic knowledgeSnapshotCreatedTopic() {
        return topic(KafkaTopics.KNOWLEDGE_GRAPH_SNAPSHOT_CREATED);
    }
}
