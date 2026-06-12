package com.athena.auth.web;

import com.athena.common.web.GlobalExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Activates the shared exception-to-HTTP mapping for this service.
 */
@RestControllerAdvice
public class AuthExceptionHandler extends GlobalExceptionHandler {
}
