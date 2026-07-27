package com.athena.auth.dto;

import java.io.Serializable;

public record TwoFactorStatusResponse(
        boolean enabled,
        String phoneNumber
) implements Serializable {
}
