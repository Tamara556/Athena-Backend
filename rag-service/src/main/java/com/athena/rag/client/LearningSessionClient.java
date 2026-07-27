package com.athena.rag.client;

import com.athena.common.security.AuthHeaders;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

@FeignClient(name = "ai-service", contextId = "learningSessionClient")
public interface LearningSessionClient {

    @GetMapping("/learning-sessions/{sessionId}")
    JsonNode getSession(@RequestHeader(AuthHeaders.USER_ID) UUID userId, @PathVariable("sessionId") UUID sessionId);
}
