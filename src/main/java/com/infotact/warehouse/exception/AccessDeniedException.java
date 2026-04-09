package com.infotact.warehouse.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an authenticated user attempts to access a resource
 * for which they do not have sufficient permissions (Role-based).
 * <p>
 * This differs from an Authentication exception in that the user's identity
 * is known, but their {@link com.infotact.warehouse.entity.enums.Role}
 * does not grant them authority for the requested operation.
 * </p>
 */
@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class AccessDeniedException extends RuntimeException {

    /**
     * Constructs a new AccessDeniedException with a specific error message.
     * * @param message The detail message explaining why access was restricted.
     */
    public AccessDeniedException(String message){
        super(message);
    }
}