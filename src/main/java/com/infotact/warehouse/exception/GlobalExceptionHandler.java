package com.infotact.warehouse.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Enhanced Global Interceptor for exception handling.
 * Optimized for RESTful responses and Bean Validation errors.
 */
@RestControllerAdvice // Modern REST approach
public class GlobalExceptionHandler {

    private Map<String, Object> createErrorBody(HttpStatus status, String message, String errorName) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", errorName);
        body.put("message", message);
        return body;
    }

    /**
     * Handles @Valid validation failures.
     * This provides the frontend with a map of exactly which fields failed validation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> body = createErrorBody(HttpStatus.BAD_REQUEST, "Validation Failed", "Constraint Violation");

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        body.put("validationErrors", errors);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ResourceNotFoundException.class, EntityNotFoundException.class})
    public ResponseEntity<Object> handleNotFound(RuntimeException ex, WebRequest request) {
        Map<String, Object> body = createErrorBody(HttpStatus.NOT_FOUND, ex.getMessage(), "Not Found");
        body.put("path", request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<Object> handleAlreadyExistsException(AlreadyExistsException ex) {
        return new ResponseEntity<>(createErrorBody(HttpStatus.CONFLICT, ex.getMessage(), "Conflict"), HttpStatus.CONFLICT);
    }

    @ExceptionHandler({
            BadRequestException.class,
            IllegalOperationException.class,
            InsufficientStorageException.class
    })
    public ResponseEntity<Object> handleBadRequest(RuntimeException ex) {
        return new ResponseEntity<>(createErrorBody(HttpStatus.BAD_REQUEST, ex.getMessage(), "Bad Request"), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleUnauthorized(UnauthorizedException ex) {
        return new ResponseEntity<>(createErrorBody(HttpStatus.UNAUTHORIZED, ex.getMessage(), "Unauthorized"), HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles Spring Security @PreAuthorize failures.
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(Exception ex) {
        return new ResponseEntity<>(createErrorBody(HttpStatus.FORBIDDEN, "You do not have permission to perform this action", "Forbidden"), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGlobalException(Exception ex) {
        // Log the actual exception for debugging
        ex.printStackTrace();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("message", "An unexpected error occurred. Please contact system administrator.");
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}