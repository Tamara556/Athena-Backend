package com.athena.common.security;

/**
 * Distinguishes short-lived access tokens from long-lived refresh tokens.
 * The type is embedded as a claim so a refresh token can never be used as an
 * access token (and vice-versa).
 */
public enum TokenType {
    ACCESS,
    REFRESH
}
