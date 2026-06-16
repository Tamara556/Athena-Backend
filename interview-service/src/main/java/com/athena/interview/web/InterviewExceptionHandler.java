package com.athena.interview.web;

import com.athena.common.web.ApiError;
import com.athena.common.web.GlobalExceptionHandler;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class InterviewExceptionHandler extends GlobalExceptionHandler {

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiError> handleFeign(FeignException ex, WebRequest request) {
        log.error("Downstream AI call failed status={}", ex.status());
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        String path = request.getDescription(false).replaceFirst("^uri=", "");
        ApiError body = ApiError.of(status.value(), status.getReasonPhrase(),
                "The AI engine is temporarily unavailable. Please try again shortly.", path);
        return ResponseEntity.status(status).body(body);
    }
}
