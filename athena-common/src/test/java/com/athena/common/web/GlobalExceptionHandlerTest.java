package com.athena.common.web;

import com.athena.common.exception.DuplicateResourceException;
import com.athena.common.exception.InvalidCredentialsException;
import com.athena.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private static final class TestHandler extends GlobalExceptionHandler {
    }

    private final TestHandler handler = new TestHandler();
    private WebRequest request;

    @BeforeEach
    void setUp() {
        request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/resource/42");
    }

    @Test
    void mapsNotFoundTo404WithMessageAndPath() {
        ResponseEntity<ApiError> response =
                handler.handleNotFound(ResourceNotFoundException.of("Roadmap", 42), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).isEqualTo("Roadmap not found: 42");
        assertThat(response.getBody().path()).isEqualTo("/api/resource/42");
        assertThat(response.getBody().details()).isEmpty();
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void mapsDuplicateTo409() {
        ResponseEntity<ApiError> response =
                handler.handleDuplicate(new DuplicateResourceException("email taken"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo("email taken");
    }

    @Test
    void mapsInvalidCredentialsTo401() {
        ResponseEntity<ApiError> response =
                handler.handleInvalidCredentials(new InvalidCredentialsException("bad password"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().status()).isEqualTo(401);
    }

    @Test
    void mapsIllegalArgumentTo400() {
        ResponseEntity<ApiError> response =
                handler.handleIllegalArgument(new IllegalArgumentException("bad input"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("bad input");
    }

    @Test
    void mapsBindingErrorTo400() {
        ResponseEntity<ApiError> response = handler.handleBindingError(
                new ServletRequestBindingException("missing header"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void mapsUnexpectedTo500WithGenericMessage() {
        ResponseEntity<ApiError> response =
                handler.handleUnexpected(new RuntimeException("leak me not"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
        // The raw exception message must never reach the client.
        assertThat(response.getBody().message()).doesNotContain("leak me not");
    }

    @Test
    void mapsValidationErrorsToFieldViolations() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("registerRequest", "email", "must not be blank"),
                new FieldError("registerRequest", "password", "too short")));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiError> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Validation failed for one or more fields");
        assertThat(response.getBody().details()).extracting(ApiError.FieldViolation::field)
                .containsExactly("email", "password");
        assertThat(response.getBody().details()).extracting(ApiError.FieldViolation::message)
                .containsExactly("must not be blank", "too short");
    }

    @Test
    void stripsUriPrefixFromPathAndToleratesNoPrefix() {
        when(request.getDescription(false)).thenReturn("/plain/path");
        ResponseEntity<ApiError> response =
                handler.handleIllegalArgument(new IllegalArgumentException("x"), request);
        assertThat(response.getBody().path()).isEqualTo("/plain/path");
    }
}
