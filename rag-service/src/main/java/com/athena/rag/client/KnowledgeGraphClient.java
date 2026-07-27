package com.athena.rag.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

@FeignClient(name = "ai-service", contextId = "knowledgeGraphClient")
public interface KnowledgeGraphClient {

    @GetMapping("/ai/knowledge-graph/{userId}")
    JsonNode getGraph(@PathVariable("userId") UUID userId);
}
