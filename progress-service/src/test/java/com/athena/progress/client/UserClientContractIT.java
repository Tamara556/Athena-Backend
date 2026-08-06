package com.athena.progress.client;

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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Consumer contract test for {@link UserClient}. WireMock is unavailable offline in this
 * environment, so a minimal JDK {@link HttpServer} acts as the stubbed provider: it records the
 * incoming request (verifying the request contract) and returns a controlled response
 * (verifying response decoding and the error contract).
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
class UserClientContractIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withStartupTimeout(Duration.ofSeconds(120));

    private static final HttpServer STUB = createStub();

    // Mutable stub state configured per test.
    private static volatile int responseStatus = 200;
    private static volatile String responseBody = "{}";
    private static volatile String lastPath;
    private static volatile String lastMethod;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        int stubPort = STUB.getAddress().getPort();
        registry.add("spring.cloud.discovery.client.simple.instances.user-service[0].uri",
                () -> "http://localhost:" + stubPort);
    }

    @Autowired
    private UserClient userClient;

    @Test
    void issuesGetToUsersPathAndDecodesTheResponseBody() {
        UUID id = UUID.randomUUID();
        responseStatus = 200;
        responseBody = "{\"userId\":\"" + id + "\",\"name\":\"Ada Lovelace\"}";

        UserSummary summary = userClient.getUser(id);

        assertThat(summary.userId()).isEqualTo(id);
        assertThat(summary.name()).isEqualTo("Ada Lovelace");
        // Request contract: GET /users/{id}
        assertThat(lastMethod).isEqualTo("GET");
        assertThat(lastPath).isEqualTo("/users/" + id);
    }

    @Test
    void mapsProviderNotFoundToFeignNotFoundException() {
        responseStatus = 404;
        responseBody = "{}";

        assertThatThrownBy(() -> userClient.getUser(UUID.randomUUID()))
                .isInstanceOf(FeignException.NotFound.class);
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
