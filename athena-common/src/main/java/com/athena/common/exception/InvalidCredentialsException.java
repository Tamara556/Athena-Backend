package com.athena.common.exception;

/**
 * Thrown when authentication fails (bad username/password or an invalid /
 * expired token). Mapped to HTTP 401 by the global exception handler.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
