package com.athena.interview.client;

import com.athena.interview.client.AiDtos.AiEvaluateRequest;
import com.athena.interview.client.AiDtos.AiEvaluation;
import com.athena.interview.client.AiDtos.AiQuestions;
import com.athena.interview.client.AiDtos.AiQuestionsRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ai-service", path = "/ai/interviews")
public interface AiInterviewClient {

    @PostMapping("/questions")
    AiQuestions generateQuestions(@RequestBody AiQuestionsRequest request);

    @PostMapping("/evaluate")
    AiEvaluation evaluate(@RequestBody AiEvaluateRequest request);
}
