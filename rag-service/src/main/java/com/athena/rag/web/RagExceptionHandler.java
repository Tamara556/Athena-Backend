package com.athena.rag.web;

import com.athena.common.web.ApiError;
import com.athena.common.web.GlobalExceptionHandler;
import com.athena.llm.LlmException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class RagExceptionHandler extends GlobalExceptionHandler {

    @ExceptionHandler(LlmException.class)
    public ResponseEntity<ApiError> handleLlm(LlmException ex, WebRequest request) {
        log.warn("LLM failure at {}: {}", path(request), ex.getMessage());
        ApiError body = ApiError.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                "The AI engine is temporarily unavailable. Please try again shortly.",
                path(request));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    private String path(WebRequest request) {
        String description = request.getDescription(false);
        return description.startsWith("uri=") ? description.substring(4) : description;
    }
}
