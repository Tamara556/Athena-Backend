package com.athena.progress.client;

import java.util.UUID;

/**
 * Minimal projection of the user-service profile response — only the fields
 * progress-service needs. Unknown JSON fields are ignored by default.
 */
public record UserSummary(UUID userId, String name) {
}
