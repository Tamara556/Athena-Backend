package com.athena.ai.web;

import com.athena.ai.client.AiException;
import com.athena.ai.client.AiTemporarilyUnavailableException;
import com.athena.ai.generation.dto.FallbackResponse;
import com.athena.common.web.ApiError;
import com.athena.common.web.GlobalExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class AiExceptionHandler extends GlobalExceptionHandler {

    @ExceptionHandler(AiTemporarilyUnavailableException.class)
    public ResponseEntity<FallbackResponse> handleTemporarilyUnavailable(AiTemporarilyUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(FallbackResponse.temporarilyUnavailable(ex.getRetryId()));
    }

    @ExceptionHandler(AccessForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(AccessForbiddenException ex, WebRequest request) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        String path = request.getDescription(false).replaceFirst("^uri=", "");
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), status.getReasonPhrase(), ex.getMessage(), path));
    }

    @ExceptionHandler(AiException.class)
    public ResponseEntity<ApiError> handleAiFailure(AiException ex, WebRequest request) {
        log.error("AI provider failure: {}", ex.getMessage());
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        String path = request.getDescription(false).replaceFirst("^uri=", "");
        ApiError body = ApiError.of(status.value(), status.getReasonPhrase(),
                "Athena's AI engine is temporarily unavailable. Please try again shortly.", path);
        return ResponseEntity.status(status).body(body);
    }
}
