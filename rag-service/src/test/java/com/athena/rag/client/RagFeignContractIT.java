package com.athena.rag.client;

import com.athena.rag.client.dto.RoadmapView;
import com.sun.net.httpserver.HttpServer;
import feign.FeignException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Consumer contract tests for rag-service's Feign clients against a stubbed provider (JDK
 * {@link HttpServer}; WireMock is unavailable offline). Both {@code ai-service} and
 * {@code progress-service} are pointed at the stub via Spring Cloud simple-discovery so the real
 * LoadBalancer/Feign/decoder stack is exercised.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "eureka.client.enabled=false",
                "spring.kafka.listener.auto-startup=false",
                "spring.cache.type=none",
                "spring.jpa.hibernate.ddl-auto=validate"
        })
@Testcontainers
class RagFeignContractIT {

    @Container
    static final PostgreSQLContainer<?> PGVECTOR = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"))
            .withStartupTimeout(Duration.ofSeconds(120));

    private static final HttpServer STUB = createStub();

    private static volatile int responseStatus = 200;
    private static volatile String responseBody = "{}";
    private static volatile String lastPath;
    private static volatile String lastMethod;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PGVECTOR::getJdbcUrl);
        registry.add("spring.datasource.username", PGVECTOR::getUsername);
        registry.add("spring.datasource.password", PGVECTOR::getPassword);
        String stubUri = "http://localhost:" + STUB.getAddress().getPort();
        registry.add("spring.cloud.discovery.client.simple.instances.ai-service[0].uri", () -> stubUri);
        registry.add("spring.cloud.discovery.client.simple.instances.progress-service[0].uri", () -> stubUri);
    }

    @Autowired
    private KnowledgeGraphClient knowledgeGraphClient;
    @Autowired
    private RoadmapClient roadmapClient;
    @Autowired
    private ProgressClient progressClient;

    @Test
    void knowledgeGraphClientRequestsGraphPathAndDecodesJson() {
        UUID userId = UUID.randomUUID();
        responseStatus = 200;
        responseBody = "{\"domain\":\"mathematics\",\"averageMastery\":72}";

        JsonNode graph = knowledgeGraphClient.getGraph(userId);

        assertThat(graph.get("domain").asText()).isEqualTo("mathematics");
        assertThat(graph.get("averageMastery").asInt()).isEqualTo(72);
        assertThat(lastMethod).isEqualTo("GET");
        assertThat(lastPath).isEqualTo("/ai/knowledge-graph/" + userId);
    }

    @Test
    void roadmapClientRequestsRoadmapPathAndDecodesView() {
        UUID id = UUID.randomUUID();
        responseStatus = 200;
        responseBody = "{\"id\":\"" + id + "\",\"goal\":\"Learn SQL\",\"level\":\"BEGINNER\","
                + "\"phases\":[{\"name\":\"Foundations\",\"description\":\"d\","
                + "\"objectives\":[\"joins\"],\"status\":\"CURRENT\"}]}";

        RoadmapView roadmap = roadmapClient.getRoadmap(id);

        assertThat(roadmap.id()).isEqualTo(id);
        assertThat(roadmap.goal()).isEqualTo("Learn SQL");
        assertThat(roadmap.level()).isEqualTo("BEGINNER");
        assertThat(roadmap.phases()).singleElement()
                .satisfies(phase -> {
                    assertThat(phase.name()).isEqualTo("Foundations");
                    assertThat(phase.status()).isEqualTo("CURRENT");
                    assertThat(phase.objectives()).containsExactly("joins");
                });
        assertThat(lastPath).isEqualTo("/ai/roadmaps/" + id);
    }

    @Test
    void progressClientRequestsProgressPathAndDecodesJson() {
        UUID userId = UUID.randomUUID();
        responseStatus = 200;
        responseBody = "{\"currentStreak\":5,\"totalCompletedTasks\":40}";

        JsonNode progress = progressClient.getProgress(userId);

        assertThat(progress.get("currentStreak").asInt()).isEqualTo(5);
        assertThat(lastPath).isEqualTo("/progress/" + userId);
    }

    @Test
    void clientPropagatesProviderServerErrorAsFeignException() {
        responseStatus = 500;
        responseBody = "{}";

        assertThatThrownBy(() -> knowledgeGraphClient.getGraph(UUID.randomUUID()))
                .isInstanceOf(FeignException.class);
    }

    private static HttpServer createStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/", exchange -> {
                lastPath = exchange.getRequestURI().getPath();
                lastMethod = exchange.getRequestMethod();
                byte[] payload = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(responseStatus, payload.length);
                try (OutputStream body = exchange.getResponseBody()) {
                    body.write(payload);
                }
            });
            server.start();
            return server;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not start stub provider", ex);
        }
    }

    @AfterAll
    static void stopStub() {
        STUB.stop(0);
    }
}
