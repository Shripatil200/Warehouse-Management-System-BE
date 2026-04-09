package com.infotact.warehouse.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a resource creation or update request conflicts
 * with existing data in the system.
 * <p>
 * This is primarily used to enforce unique constraints at the service level,
 * such as duplicate Warehouse names, Product SKUs, or User emails.
 * It maps to an <b>HTTP 409 Conflict</b> response.
 * </p>
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class AlreadyExistsException extends RuntimeException {

    /**
     * Constructs a new AlreadyExistsException with a specific detail message.
     * * @param message The error message explaining which unique constraint was violated.
     */
    public AlreadyExistsException(String message) {
        super(message);
    }
}