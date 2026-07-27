package com.athena.ai.dailyjourney.controller;

import com.athena.ai.dailyjourney.domain.AdjustAction;
import com.athena.ai.dailyjourney.domain.ConfidenceLevel;
import com.athena.ai.dailyjourney.dto.AdjustPlanRequest;
import com.athena.ai.dailyjourney.dto.AdjustTimeRequest;
import com.athena.ai.dailyjourney.dto.BlockProgressRequest;
import com.athena.ai.dailyjourney.dto.BlockSkipRequest;
import com.athena.ai.dailyjourney.dto.CheckinRequest;
import com.athena.ai.dailyjourney.dto.DailyJourneyResponse;
import com.athena.ai.dailyjourney.dto.ReflectionRequest;
import com.athena.ai.generation.model.WhyReasoning;
import com.athena.ai.dailyjourney.service.DailyJourneyService;
import com.athena.common.security.AuthHeaders;
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
@RequestMapping("/daily-journey")
@RequiredArgsConstructor
public class DailyJourneyController {

    private final DailyJourneyService dailyJourneyService;

    @GetMapping("/today")
    public ResponseEntity<DailyJourneyResponse> today(@RequestHeader(AuthHeaders.USER_ID) UUID userId) {
        return ResponseEntity.ok(dailyJourneyService.getToday(userId));
    }

    @GetMapping("/today/why")
    public ResponseEntity<WhyReasoning> why(@RequestHeader(AuthHeaders.USER_ID) UUID userId) {
        return ResponseEntity.ok(dailyJourneyService.getWhy(userId));
    }

    @PostMapping("/today/start")
    public ResponseEntity<DailyJourneyResponse> startDay(@RequestHeader(AuthHeaders.USER_ID) UUID userId) {
        return ResponseEntity.ok(dailyJourneyService.startDay(userId));
    }

    @PostMapping("/today/adjust")
    public ResponseEntity<DailyJourneyResponse> adjust(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                       @Valid @RequestBody AdjustPlanRequest request) {
        return ResponseEntity.ok(dailyJourneyService.adjustPlan(userId, AdjustAction.fromString(request.action())));
    }

    @PostMapping("/today/time")
    public ResponseEntity<DailyJourneyResponse> time(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                     @Valid @RequestBody AdjustTimeRequest request) {
        return ResponseEntity.ok(dailyJourneyService.adjustTime(userId, request.availableMinutes()));
    }

    @PostMapping("/blocks/{blockId}/start")
    public ResponseEntity<DailyJourneyResponse> startBlock(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                           @PathVariable UUID blockId) {
        return ResponseEntity.ok(dailyJourneyService.startBlock(userId, blockId));
    }

    @PostMapping("/blocks/{blockId}/progress")
    public ResponseEntity<DailyJourneyResponse> progress(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                         @PathVariable UUID blockId,
                                                         @Valid @RequestBody BlockProgressRequest request) {
        return ResponseEntity.ok(dailyJourneyService.updateProgress(userId, blockId, request.percent()));
    }

    @PostMapping("/blocks/{blockId}/complete")
    public ResponseEntity<DailyJourneyResponse> completeBlock(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                              @PathVariable UUID blockId) {
        return ResponseEntity.ok(dailyJourneyService.completeBlock(userId, blockId));
    }

    @PostMapping("/blocks/{blockId}/skip")
    public ResponseEntity<DailyJourneyResponse> skipBlock(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                          @PathVariable UUID blockId,
                                                          @RequestBody(required = false) BlockSkipRequest request) {
        return ResponseEntity.ok(dailyJourneyService.skipBlock(userId, blockId, request == null ? null : request.reason()));
    }

    @PostMapping("/blocks/{blockId}/relink")
    public ResponseEntity<DailyJourneyResponse> relinkBlock(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                            @PathVariable UUID blockId) {
        return ResponseEntity.ok(dailyJourneyService.relinkBlock(userId, blockId));
    }

    @PostMapping("/weaknesses/{knowledgeNodeId}/strengthen")
    public ResponseEntity<DailyJourneyResponse> strengthen(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                           @PathVariable UUID knowledgeNodeId) {
        return ResponseEntity.ok(dailyJourneyService.strengthen(userId, knowledgeNodeId));
    }

    @PostMapping("/checkin")
    public ResponseEntity<DailyJourneyResponse> checkin(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                        @Valid @RequestBody CheckinRequest request) {
        return ResponseEntity.ok(
                dailyJourneyService.checkin(userId, ConfidenceLevel.fromString(request.confidence()), request.blockId()));
    }

    @PostMapping("/reflection")
    public ResponseEntity<DailyJourneyResponse> reflection(@RequestHeader(AuthHeaders.USER_ID) UUID userId,
                                                           @Valid @RequestBody ReflectionRequest request) {
        return ResponseEntity.ok(dailyJourneyService.saveReflection(userId, request.hardestPart(),
                request.whatClicked(), request.adjustRequest()));
    }

    @PostMapping("/reflection/skip")
    public ResponseEntity<DailyJourneyResponse> skipReflection(@RequestHeader(AuthHeaders.USER_ID) UUID userId) {
        return ResponseEntity.ok(dailyJourneyService.skipReflection(userId));
    }
}
