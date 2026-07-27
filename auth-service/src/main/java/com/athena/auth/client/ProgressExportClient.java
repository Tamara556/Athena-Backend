package com.athena.auth.client;

import com.athena.common.security.AuthHeaders;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "progress-service", contextId = "progressExportClient")
public interface ProgressExportClient {

    @GetMapping("/progress/me")
    JsonNode progress(@RequestHeader(AuthHeaders.USER_ID) UUID userId);

    @GetMapping("/progress/streaks")
    JsonNode streaks(@RequestHeader(AuthHeaders.USER_ID) UUID userId);
}
