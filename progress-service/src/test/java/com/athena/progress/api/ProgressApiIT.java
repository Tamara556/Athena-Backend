package com.athena.progress.api;

import com.athena.common.security.AuthHeaders;
import com.athena.progress.entity.LearningProgress;
import com.athena.progress.repository.LearningProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.kafka.listener.auto-startup=false",
                "spring.cache.type=none",
                "spring.jpa.hibernate.ddl-auto=validate"
        })
@Testcontainers
class ProgressApiIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withStartupTimeout(Duration.ofSeconds(120));

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private final ObjectMapper mapper = JsonMapper.builder().build();

    @LocalServerPort
    private int port;
    private RestClient client;

    @Autowired
    private LearningProgressRepository progressRepository;

    @BeforeEach
    void setUp() {
        client = RestClient.create("http://localhost:" + port);
    }

    @Test
    void returnsOwnProgressWhenUserHeaderIsPresent() {
        UUID userId = UUID.randomUUID();
        LearningProgress progress = new LearningProgress(userId);
        progress.setTotalCompletedTasks(7);
        progress.setTotalMinutes(200);
        progress.setCurrentStreak(3);
        progress.setLongestStreak(5);
        progressRepository.saveAndFlush(progress);

        ResponseEntity<String> response = getMe(userId.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        JsonNode body = json(response);
        assertThat(body.get("userId").asText()).isEqualTo(userId.toString());
        assertThat(body.get("totalCompletedTasks").asInt()).isEqualTo(7);
        assertThat(body.get("currentStreak").asInt()).isEqualTo(3);
        assertThat(body.get("longestStreak").asInt()).isEqualTo(5);
    }

    @Test
    void rejectsRequestWithoutUserHeaderAsBadRequest() {
        ResponseEntity<String> response = client.get().uri("/progress/me")
                .retrieve()
                .onStatus(status -> status.isError(), (req, res) -> { })
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void returnsNotFoundWhenUserHasNoProgress() {
        ResponseEntity<String> response = getMe(UUID.randomUUID().toString());

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(json(response).get("status").asInt()).isEqualTo(404);
    }

    private ResponseEntity<String> getMe(String userId) {
        return client.get().uri("/progress/me")
                .header(AuthHeaders.USER_ID, userId)
                .retrieve()
                .onStatus(status -> status.isError(), (req, res) -> { })
                .toEntity(String.class);
    }

    private JsonNode json(ResponseEntity<String> response) {
        return mapper.readTree(response.getBody());
    }
}
