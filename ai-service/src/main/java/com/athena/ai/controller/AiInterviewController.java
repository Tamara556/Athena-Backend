package com.athena.ai.controller;

import com.athena.ai.dto.EvaluateInterviewRequest;
import com.athena.ai.dto.GenerateInterviewQuestionsRequest;
import com.athena.ai.model.InterviewEvaluation;
import com.athena.ai.model.InterviewQuestions;
import com.athena.ai.service.InterviewAiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/interviews")
@RequiredArgsConstructor
public class AiInterviewController {

    private final InterviewAiService interviewAiService;

    @PostMapping("/questions")
    public ResponseEntity<InterviewQuestions> questions(@Valid @RequestBody GenerateInterviewQuestionsRequest request) {
        return ResponseEntity.ok(interviewAiService.generateQuestions(request));
    }

    @PostMapping("/evaluate")
    public ResponseEntity<InterviewEvaluation> evaluate(@Valid @RequestBody EvaluateInterviewRequest request) {
        return ResponseEntity.ok(interviewAiService.evaluate(request));
    }
}
