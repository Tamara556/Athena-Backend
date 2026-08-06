package com.athena.auth.api;

import com.athena.auth.messaging.AuthEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.Duration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.kafka.listener.auto-startup=false",
                "spring.jpa.hibernate.ddl-auto=validate"
        })
@Testcontainers
class AuthApiIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withStartupTimeout(Duration.ofSeconds(120));

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    // Neutralise external I/O so the HTTP API surface is what is under test.
    @MockitoBean
    private AuthEventPublisher eventPublisher;
    @MockitoBean
    private S3Client s3Client;

    private static final AtomicInteger SEQ = new AtomicInteger();
    private final ObjectMapper mapper = JsonMapper.builder().build();

    @LocalServerPort
    private int port;
    private RestClient client;

    @BeforeEach
    void setUp() {
        client = RestClient.create("http://localhost:" + port);
    }

    @Test
    void registerCreatesAccountAndReturnsTokens() {
        String user = uniqueUser();
        ResponseEntity<String> response = registerRaw(user, user + "@example.com", "Password123!");

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        JsonNode body = json(response);
        assertThat(body.get("username").asText()).isEqualTo(user);
        assertThat(body.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(body.get("accessToken").asText()).isNotBlank();
        assertThat(body.get("refreshToken").asText()).isNotBlank();
        assertThat(body.get("roles").get(0).asText()).isEqualTo("USER");
    }

    @Test
    void registerRejectsDuplicateEmailWithConflict() {
        String email = uniqueUser() + "@example.com";
        assertThat(registerRaw(uniqueUser(), email, "Password123!").getStatusCode().value()).isEqualTo(201);

        ResponseEntity<String> duplicate = registerRaw(uniqueUser(), email, "Password123!");
        assertThat(duplicate.getStatusCode().value()).isEqualTo(409);
        assertThat(json(duplicate).get("status").asInt()).isEqualTo(409);
    }

    @Test
    void registerRejectsWeakPasswordWithFieldViolation() {
        ResponseEntity<String> response = registerRaw(uniqueUser(), uniqueUser() + "@example.com", "short");
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        JsonNode body = json(response);
        assertThat(body.get("status").asInt()).isEqualTo(400);
        assertThat(body.get("details").get(0).get("field").asText()).isEqualTo("password");
    }

    @Test
    void registerRejectsInvalidUsername() {
        ResponseEntity<String> response = registerRaw("has space", uniqueUser() + "@example.com", "Password123!");
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(json(response).get("details").get(0).get("field").asText()).isEqualTo("username");
    }

    @Test
    void loginSucceedsAfterRegistration() {
        String user = uniqueUser();
        registerRaw(user, user + "@example.com", "Password123!");

        ResponseEntity<String> response = postJson("/auth/login", Map.of("login", user, "password", "Password123!"));
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(json(response).get("username").asText()).isEqualTo(user);
        assertThat(json(response).get("accessToken").asText()).isNotBlank();
    }

    @Test
    void loginWithWrongPasswordIsUnauthorized() {
        String user = uniqueUser();
        registerRaw(user, user + "@example.com", "Password123!");

        ResponseEntity<String> response = postJson("/auth/login", Map.of("login", user, "password", "WrongPassword1!"));
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(json(response).get("status").asInt()).isEqualTo(401);
    }

    @Test
    void loginWithUnknownUserIsUnauthorized() {
        ResponseEntity<String> response =
                postJson("/auth/login", Map.of("login", "ghost@example.com", "password", "Password123!"));
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void loginWithBlankFieldsIsBadRequest() {
        ResponseEntity<String> response = postJson("/auth/login", Map.of("login", "", "password", ""));
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void refreshRotatesTokensForValidRefreshToken() {
        String user = uniqueUser();
        String refreshToken = json(registerRaw(user, user + "@example.com", "Password123!"))
                .get("refreshToken").asText();

        ResponseEntity<String> response = postJson("/auth/refresh", Map.of("refreshToken", refreshToken));
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(json(response).get("accessToken").asText()).isNotBlank();
        assertThat(json(response).get("refreshToken").asText()).isNotBlank();
    }

    @Test
    void refreshWithInvalidTokenIsUnauthorized() {
        ResponseEntity<String> response = postJson("/auth/refresh", Map.of("refreshToken", "not-a-real-token"));
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    private ResponseEntity<String> registerRaw(String username, String email, String password) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("firstName", "Ada");
        form.add("lastName", "Lovelace");
        form.add("username", username);
        form.add("email", email);
        form.add("password", password);
        return client.post().uri("/auth/register")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .onStatus(status -> status.isError(), (req, res) -> { })
                .toEntity(String.class);
    }

    private ResponseEntity<String> postJson(String path, Map<String, String> body) {
        return client.post().uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(status -> status.isError(), (req, res) -> { })
                .toEntity(String.class);
    }

    private JsonNode json(ResponseEntity<String> response) {
        return mapper.readTree(response.getBody());
    }

    private String uniqueUser() {
        return "user" + System.nanoTime() + SEQ.incrementAndGet();
    }
}
