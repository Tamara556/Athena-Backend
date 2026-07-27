package com.athena.rag.client;

import com.athena.rag.client.dto.RoadmapView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "ai-service", contextId = "roadmapClient")
public interface RoadmapClient {

    @GetMapping("/ai/roadmaps/{id}")
    RoadmapView getRoadmap(@PathVariable("id") UUID id);
}
