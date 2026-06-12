package com.athena.progress.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Declarative client for user-service, resolved via Eureka ({@code lb://user-service}).
 * Used to validate that a profile exists before progress is first recorded.
 */
@FeignClient(name = "user-service", path = "/users")
public interface UserClient {

    @GetMapping("/{id}")
    UserSummary getUser(@PathVariable("id") UUID id);
}
