package com.athena.learning.controller;

import com.athena.common.security.AuthHeaders;
import com.athena.learning.dto.CreatePlanRequest;
import com.athena.learning.dto.PlanResponse;
import com.athena.learning.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @PostMapping("/plans")
    public ResponseEntity<PlanResponse> create(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                               @Valid @RequestBody CreatePlanRequest request,
                                               UriComponentsBuilder uriBuilder) {
        PlanResponse created = planService.create(userId, request);
        URI location = uriBuilder.path("/plans/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/plans/{id}")
    public ResponseEntity<PlanResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(planService.getById(id));
    }

    @GetMapping("/users/{userId}/plans")
    public ResponseEntity<List<PlanResponse>> getByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(planService.getByUser(userId));
    }
}
