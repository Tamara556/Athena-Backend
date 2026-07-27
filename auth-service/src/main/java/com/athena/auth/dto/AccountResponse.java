package com.athena.auth.dto;

import java.util.UUID;

public record AccountResponse(
        UUID userId,
        String firstName,
        String lastName,
        String username,
        String email,
        String imageName
) {
}
