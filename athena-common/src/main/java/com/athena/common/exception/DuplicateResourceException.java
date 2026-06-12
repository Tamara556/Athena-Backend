package com.athena.common.exception;

/**
 * Thrown when creating a resource that violates a uniqueness constraint.
 * Mapped to HTTP 409 by each service's global exception handler.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
