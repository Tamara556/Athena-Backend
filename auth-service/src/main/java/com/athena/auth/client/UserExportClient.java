package com.athena.auth.client;

import com.athena.common.security.AuthHeaders;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "user-service", contextId = "userExportClient")
public interface UserExportClient {

    @GetMapping("/users/me/settings")
    JsonNode settings(@RequestHeader(AuthHeaders.USER_ID) UUID userId);
}
