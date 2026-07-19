package com.kemet.core.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

/**
 * Centralized exception handling for the Kemet API.
 *
 * HTTP status choices:
 * - 502 Bad Gateway for IOException/InterruptedException originating from the companion
 *   (OpenAI) call: this service acts as a gateway to OpenAI; 502 conveys "we received
 *   an invalid or no response from the upstream service" rather than 503 ("we ourselves
 *   are unavailable").
 * - 500 Internal Server Error for IllegalStateException (e.g., unseeded faculty data) —
 *   indicates a server-side configuration or data problem, not a transient upstream issue.
 * - ResponseStatusException is re-thrown so Spring's ResponseStatusExceptionResolver
 *   continues to handle it at its native status (e.g., 400 from UserController).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({IOException.class, InterruptedException.class})
    public ResponseEntity<Map<String, String>> handleCompanionUnavailable(Exception ex) {
        if (ex instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of(
                        "error", "companion_unavailable",
                        "message", "The AI companion is temporarily unavailable. Please try again."
                ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "internal_error",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception ex) throws Exception {
        if (ex instanceof ResponseStatusException) {
            throw ex; // let Spring's ResponseStatusExceptionResolver handle this normally
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "unexpected_error",
                        "message", "An unexpected error occurred."
                ));
    }
}
