package com.athena.common.exception;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionsTest {

    @Test
    void resourceNotFoundFactoryFormatsResourceAndId() {
        UUID id = UUID.randomUUID();
        ResourceNotFoundException ex = ResourceNotFoundException.of("Learning session", id);
        assertThat(ex).hasMessage("Learning session not found: " + id);
    }

    @Test
    void resourceNotFoundIsRuntimeException() {
        assertThat(new ResourceNotFoundException("x")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void duplicateResourceCarriesMessage() {
        assertThat(new DuplicateResourceException("email taken")).hasMessage("email taken")
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void invalidCredentialsCarriesMessage() {
        assertThat(new InvalidCredentialsException("nope")).hasMessage("nope")
                .isInstanceOf(RuntimeException.class);
    }
}
