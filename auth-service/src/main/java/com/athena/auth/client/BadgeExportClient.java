package com.athena.auth.client;

import com.athena.common.security.AuthHeaders;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "badge-service", contextId = "badgeExportClient")
public interface BadgeExportClient {

    @GetMapping("/badges/me")
    JsonNode badges(@RequestHeader(AuthHeaders.USER_ID) UUID userId);
}
