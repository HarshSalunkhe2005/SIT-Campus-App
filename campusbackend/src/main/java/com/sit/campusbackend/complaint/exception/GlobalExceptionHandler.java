package com.sit.campusbackend.complaint.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralised error handler — converts exceptions into structured JSON responses.
 *
 * Response shape:
 * {
 *   "timestamp": "...",
 *   "status": 400,
 *   "error": "Human-readable message"
 * }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 404 — resource not found in the database. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(ex.getMessage(), 404));
    }

    /** 400 — caller passed invalid arguments programmatically. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(ex.getMessage(), 400));
    }

    /**
     * 400 — @Valid annotation failed on a request body field.
     * Collects every failed field and its constraint message into one readable string.
     * e.g. "title: Title must be between 5 and 100 characters; email: Must be a valid email address"
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(fieldErrors, 400));
    }

    /** 500 — unexpected server-side error. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("An unexpected error occurred: " + ex.getMessage(), 500));
    }

    // ── private ───────────────────────────────────────────────────────────────

    private Map<String, Object> errorBody(String message, int status) {
        return Map.of(
            "timestamp", LocalDateTime.now().toString(),
            "status",    status,
            "error",     message
        );
    }
}
