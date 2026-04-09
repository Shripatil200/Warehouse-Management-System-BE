package com.infotact.warehouse.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global Interceptor for exception handling across the entire Warehouse API.
 * <p>
 * This class catches specific business exceptions and formats them into a
 * standardized JSON error structure. This ensures a consistent contract
 * with the frontend team and prevents sensitive internal stack traces from
 * leaking into the HTTP responses.
 * </p>
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Standard error structure generator.
     */
    private Map<String, Object> createErrorBody(HttpStatus status, String message, String errorName) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", errorName);
        body.put("message", message);
        return body;
    }

    /**
     * Handles missing resources (404 Not Found).
     */
    @ExceptionHandler({ResourceNotFoundException.class, EntityNotFoundException.class})
    public ResponseEntity<Object> handleNotFound(RuntimeException ex, WebRequest request) {
        Map<String, Object> body = createErrorBody(HttpStatus.NOT_FOUND, ex.getMessage(), "Not Found");
        body.put("path", request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles data conflicts, such as duplicate SKUs or Names (409 Conflict).
     */
    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<Object> handleAlreadyExistsException(AlreadyExistsException ex) {
        return new ResponseEntity<>(createErrorBody(HttpStatus.CONFLICT, ex.getMessage(), "Conflict"), HttpStatus.CONFLICT);
    }

    /**
     * Handles invalid business logic requests (400 Bad Request).
     */
    @ExceptionHandler({
            BadRequestException.class,
            IllegalOperationException.class,
            InsufficientStorageException.class
    })
    public ResponseEntity<Object> handleBadRequest(RuntimeException ex) {
        return new ResponseEntity<>(createErrorBody(HttpStatus.BAD_REQUEST, ex.getMessage(), "Bad Request"), HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles security authentication failures (401 Unauthorized).
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleUnauthorized(UnauthorizedException ex) {
        return new ResponseEntity<>(createErrorBody(HttpStatus.UNAUTHORIZED, ex.getMessage(), "Unauthorized"), HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles role-based permission failures (403 Forbidden).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex) {
        return new ResponseEntity<>(createErrorBody(HttpStatus.FORBIDDEN, ex.getMessage(), "Forbidden"), HttpStatus.FORBIDDEN);
    }

    /**
     * Catch-all for unhandled server-side errors (500 Internal Server Error).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGlobalException(Exception ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("message", "An unexpected error occurred. Please contact system administrator.");
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}