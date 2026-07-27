package com.athena.rag.profile.controller;

import com.athena.common.security.AuthHeaders;
import com.athena.rag.profile.dto.MemoryProfileResponse;
import com.athena.rag.profile.service.MemoryProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/ai/memory")
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryProfileService memoryProfileService;

    @GetMapping("/me")
    public ResponseEntity<MemoryProfileResponse> myMemory(@RequestHeader(AuthHeaders.USER_ID) UUID userId) {
        return ResponseEntity.ok(memoryProfileService.getProfile(userId));
    }
}
