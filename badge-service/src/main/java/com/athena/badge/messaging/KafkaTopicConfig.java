package com.athena.badge.messaging;

import com.athena.common.event.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic badgeAwardedTopic() {
        return TopicBuilder.name(KafkaTopics.BADGE_AWARDED).partitions(1).replicas(1).build();
    }
}
