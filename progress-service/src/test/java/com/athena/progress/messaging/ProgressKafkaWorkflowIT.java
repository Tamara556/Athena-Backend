package com.athena.progress.messaging;

import com.athena.common.event.InterviewEvaluatedEvent;
import com.athena.common.event.KafkaTopics;
import com.athena.common.event.StreakUpdatedEvent;
import com.athena.common.event.TaskCompletedEvent;
import com.athena.progress.entity.DailyProgress;
import com.athena.progress.entity.LearningProgress;
import com.athena.progress.repository.DailyProgressRepository;
import com.athena.progress.repository.LearningProgressRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cache.type=none",
                "spring.jpa.hibernate.ddl-auto=validate"
        })
@Testcontainers
class ProgressKafkaWorkflowIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withStartupTimeout(Duration.ofSeconds(120));

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"))
            .withStartupTimeout(Duration.ofSeconds(120));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private LearningProgressRepository progressRepository;
    @Autowired
    private DailyProgressRepository dailyProgressRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void taskCompletedEventUpdatesProgressAndEmitsStreakEvent() {
        UUID userId = UUID.randomUUID();
        progressRepository.saveAndFlush(new LearningProgress(userId));

        try (KafkaConsumer<String, String> streakConsumer = consumerFor(KafkaTopics.STREAK_UPDATED)) {
            streakConsumer.poll(Duration.ofMillis(500)); // force partition assignment before we publish

            TaskCompletedEvent event = new TaskCompletedEvent(
                    userId, UUID.randomUUID(), UUID.randomUUID(), "LESSON", 30, Instant.now());
            kafkaTemplate.send(KafkaTopics.TASK_COMPLETED, userId.toString(), objectMapper.writeValueAsString(event));

            // Consumer deserialized the event and updated persistent state.
            await().atMost(Duration.ofSeconds(30)).until(() -> progressRepository.findById(userId)
                    .map(p -> p.getTotalCompletedTasks() == 1).orElse(false));
            LearningProgress progress = progressRepository.findById(userId).orElseThrow();
            assertThat(progress.getTotalMinutes()).isEqualTo(30);
            assertThat(progress.getCurrentStreak()).isEqualTo(1);

            // The workflow re-published a downstream StreakUpdatedEvent, correctly serialized.
            StreakUpdatedEvent streak = pollForUser(streakConsumer, userId);
            assertThat(streak).isNotNull();
            assertThat(streak.currentStreak()).isEqualTo(1);
            assertThat(streak.completedTasks()).isEqualTo(1);
        }
    }

    @Test
    void interviewEvaluatedEventRecordsDailyInterview() {
        UUID userId = UUID.randomUUID();
        InterviewEvaluatedEvent event = new InterviewEvaluatedEvent(
                userId, UUID.randomUUID(), "databases", 80, true, List.of("joins"), Instant.now());

        kafkaTemplate.send(KafkaTopics.INTERVIEW_EVALUATED, userId.toString(),
                objectMapper.writeValueAsString(event));

        await().atMost(Duration.ofSeconds(30)).until(() ->
                dailyProgressRepository.findByUserIdAndDate(userId, LocalDate.now())
                        .map(d -> d.getInterviewsCompleted() >= 1).orElse(false));
        DailyProgress daily = dailyProgressRepository.findByUserIdAndDate(userId, LocalDate.now()).orElseThrow();
        assertThat(daily.getInterviewsCompleted()).isEqualTo(1);
    }

    private StreakUpdatedEvent pollForUser(KafkaConsumer<String, String> consumer, UUID userId) {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                StreakUpdatedEvent event = objectMapper.readValue(record.value(), StreakUpdatedEvent.class);
                if (event.userId().equals(userId)) {
                    return event;
                }
            }
        }
        return null;
    }

    private KafkaConsumer<String, String> consumerFor(String topic) {
        Properties props = new Properties();
        props.putAll(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "verifier-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class));
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(topic));
        return consumer;
    }
}
