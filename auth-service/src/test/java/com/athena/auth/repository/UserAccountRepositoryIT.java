package com.athena.auth.repository;

import com.athena.auth.domain.Role;
import com.athena.auth.entity.UserAccount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.kafka.listener.auto-startup=false",
                "spring.jpa.hibernate.ddl-auto=validate"
        })
@Testcontainers
@Transactional
class UserAccountRepositoryIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withStartupTimeout(Duration.ofSeconds(120));

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private UserAccountRepository repository;

    @Test
    void liquibaseSchemaAppliesAndAccountRoundTripsWithRoles() {
        UserAccount saved = repository.save(account("Ada", "Lovelace", "ada", "ada@example.com"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        Optional<UserAccount> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getRoles()).containsExactly(Role.USER);
        assertThat(found.get().isTwoFactorEnabled()).isFalse();
    }

    @Test
    void findByEmailIgnoreCaseMatchesRegardlessOfCasing() {
        repository.save(account("Grace", "Hopper", "grace", "Grace.Hopper@Example.com"));

        assertThat(repository.findByEmailIgnoreCase("grace.hopper@example.com")).isPresent();
        assertThat(repository.existsByEmailIgnoreCase("GRACE.HOPPER@EXAMPLE.COM")).isTrue();
        assertThat(repository.existsByEmailIgnoreCase("someone.else@example.com")).isFalse();
    }

    @Test
    void existsByUsernameIgnoreCaseIsCaseInsensitive() {
        repository.save(account("Alan", "Turing", "turing", "alan@example.com"));

        assertThat(repository.existsByUsernameIgnoreCase("TURING")).isTrue();
        assertThat(repository.existsByUsernameIgnoreCase("nobody")).isFalse();
    }

    @Test
    void findByEmailOrUsernameMatchesEitherIdentifier() {
        repository.save(account("Edsger", "Dijkstra", "edsger", "ed@example.com"));

        assertThat(repository.findByEmailIgnoreCaseOrUsernameIgnoreCase("ed@example.com", "ed@example.com"))
                .isPresent();
        assertThat(repository.findByEmailIgnoreCaseOrUsernameIgnoreCase("edsger", "edsger")).isPresent();
        assertThat(repository.findByEmailIgnoreCaseOrUsernameIgnoreCase("missing", "missing")).isEmpty();
    }

    private static UserAccount account(String first, String last, String username, String email) {
        UserAccount account = new UserAccount();
        account.setFirstName(first);
        account.setLastName(last);
        account.setUsername(username);
        account.setEmail(email);
        account.setPasswordHash("hash");
        account.addRole(Role.USER);
        return account;
    }
}
