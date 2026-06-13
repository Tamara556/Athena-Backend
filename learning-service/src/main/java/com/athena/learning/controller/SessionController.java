package com.athena.learning.controller;

import com.athena.common.security.AuthHeaders;
import com.athena.learning.dto.EndSessionRequest;
import com.athena.learning.dto.SessionResponse;
import com.athena.learning.dto.StartSessionRequest;
import com.athena.learning.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/start")
    public ResponseEntity<SessionResponse> start(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                 @Valid @RequestBody StartSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.start(userId, request));
    }

    @PostMapping("/end")
    public ResponseEntity<SessionResponse> end(@Valid @RequestBody EndSessionRequest request) {
        return ResponseEntity.ok(sessionService.end(request));
    }
}
