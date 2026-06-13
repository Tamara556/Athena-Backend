package com.athena.progress.controller;

import com.athena.common.security.AuthHeaders;
import com.athena.progress.dto.ProgressResponse;
import com.athena.progress.dto.ProgressUpdateRequest;
import com.athena.progress.dto.WeeklySummaryResponse;
import com.athena.progress.service.ProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @GetMapping("/me")
    public ResponseEntity<ProgressResponse> getMyProgress(@RequestHeader(AuthHeaders.USER_ID) UUID userId) {
        return ResponseEntity.ok(progressService.getProgress(userId));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ProgressResponse> getProgress(@PathVariable UUID userId) {
        return ResponseEntity.ok(progressService.getProgress(userId));
    }

    @PostMapping("/update")
    public ResponseEntity<ProgressResponse> update(@Valid @RequestBody ProgressUpdateRequest request) {
        return ResponseEntity.ok(progressService.update(request));
    }

    @GetMapping("/summary/{userId}")
    public ResponseEntity<WeeklySummaryResponse> summary(@PathVariable UUID userId) {
        return ResponseEntity.ok(progressService.weeklySummary(userId));
    }
}
