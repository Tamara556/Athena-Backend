package com.athena.auth.domain;

/**
 * Coarse-grained authorization roles. Stored per user and embedded into the
 * access token so the gateway and downstream services can authorize requests.
 */
public enum Role {
    USER,
    ADMIN
}
