package com.athena.interview.controller;

import com.athena.common.security.AuthHeaders;
import com.athena.interview.dto.InterviewResponse;
import com.athena.interview.dto.InterviewResultResponse;
import com.athena.interview.dto.StartInterviewRequest;
import com.athena.interview.dto.SubmitInterviewRequest;
import com.athena.interview.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/start")
    public ResponseEntity<InterviewResponse> start(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                   @Valid @RequestBody StartInterviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(interviewService.start(userId, request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<InterviewResponse>> mine(@RequestHeader(AuthHeaders.USER_ID) UUID userId) {
        return ResponseEntity.ok(interviewService.getForUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InterviewResponse> byId(@PathVariable UUID id) {
        return ResponseEntity.ok(interviewService.getById(id));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<InterviewResultResponse> submit(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                          @PathVariable UUID id,
                                                          @Valid @RequestBody SubmitInterviewRequest request) {
        return ResponseEntity.ok(interviewService.submit(userId, id, request));
    }
}
