package com.infotact.warehouse.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a generic system resource or API endpoint is not found.
 * <p>
 * This maps to an <b>HTTP 404 Not Found</b> response. It is typically used
 * when a user requests a resource that does not exist in the context of the
 * current request (e.g., a report that hasn't been generated yet or a specific
 * warehouse-related file).
 * </p>
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a new ResourceNotFoundException with a specific error message.
     * @param message The detail message explaining which resource is missing.
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}