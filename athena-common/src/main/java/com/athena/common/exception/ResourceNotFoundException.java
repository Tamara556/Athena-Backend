package com.athena.common.exception;

/**
 * Thrown when a requested domain resource does not exist. Mapped to HTTP 404
 * by each service's global exception handler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String resource, Object id) {
        return new ResourceNotFoundException("%s not found: %s".formatted(resource, id));
    }
}
