package com.athena.interview.messaging;

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
    NewTopic interviewStartedTopic() {
        return topic(KafkaTopics.INTERVIEW_STARTED);
    }

    @Bean
    NewTopic interviewCompletedTopic() {
        return topic(KafkaTopics.INTERVIEW_COMPLETED);
    }

    @Bean
    NewTopic interviewEvaluatedTopic() {
        return topic(KafkaTopics.INTERVIEW_EVALUATED);
    }
}
