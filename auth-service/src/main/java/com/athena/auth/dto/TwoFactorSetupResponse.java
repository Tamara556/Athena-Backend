package com.athena.auth.dto;

import java.io.Serializable;

public record TwoFactorSetupResponse(
        String phoneNumber
) implements Serializable {
}
