package com.infotact.warehouse.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a request is made without valid authentication credentials.
 * <p>
 * This maps to an <b>HTTP 401 Unauthorized</b> response. It is typically triggered
 * when a JWT is missing, expired, or malformed, signaling to the client that the
 * user must (re)authenticate to access the system.
 * </p>
 */
@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {

    /**
     * Constructs a new UnauthorizedException with a specific error message.
     * @param message The detail message explaining the authentication failure
     * (e.g., "Invalid token", "Session expired").
     */
    public UnauthorizedException(String message){
        super(message);
    }
}