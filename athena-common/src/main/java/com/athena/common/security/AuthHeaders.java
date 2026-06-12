package com.athena.common.security;

/**
 * Header names used to propagate the authenticated identity from the API
 * Gateway to downstream services. Downstream services trust these headers
 * because the gateway is the only ingress that validates the JWT.
 */
public final class AuthHeaders {

    /** Authenticated user id (JWT subject). */
    public static final String USER_ID = "X-User-Id";

    /** Comma-separated authority list, e.g. {@code USER,ADMIN}. */
    public static final String USER_ROLES = "X-User-Roles";

    private AuthHeaders() {
    }
}
