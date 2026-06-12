package com.athena.common.web;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error contract returned by every Athena service. Keeping this in the
 * shared module guarantees clients see an identical error shape regardless of
 * which service produced it.
 *
 * @param timestamp when the error was produced (UTC)
 * @param status    HTTP status code
 * @param error     HTTP reason phrase
 * @param message   human-readable, client-safe description
 * @param path      request path that produced the error
 * @param details   optional field-level validation errors (may be empty)
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldViolation> details
) {

    public record FieldViolation(String field, String message) {
    }

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, List.of());
    }

    public static ApiError of(int status, String error, String message, String path, List<FieldViolation> details) {
        return new ApiError(Instant.now(), status, error, message, path, details);
    }
}
