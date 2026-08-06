package com.athena.common.web;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorTest {

    @Test
    void fourArgFactoryLeavesDetailsEmptyAndStampsTime() {
        ApiError error = ApiError.of(404, "Not Found", "missing", "/x");

        assertThat(error.status()).isEqualTo(404);
        assertThat(error.error()).isEqualTo("Not Found");
        assertThat(error.message()).isEqualTo("missing");
        assertThat(error.path()).isEqualTo("/x");
        assertThat(error.details()).isEmpty();
        assertThat(error.timestamp()).isNotNull();
    }

    @Test
    void fiveArgFactoryPreservesFieldViolations() {
        List<ApiError.FieldViolation> details = List.of(
                new ApiError.FieldViolation("email", "must not be blank"));

        ApiError error = ApiError.of(400, "Bad Request", "Validation failed", "/register", details);

        assertThat(error.details()).hasSize(1);
        assertThat(error.details().getFirst().field()).isEqualTo("email");
        assertThat(error.details().getFirst().message()).isEqualTo("must not be blank");
    }
}
