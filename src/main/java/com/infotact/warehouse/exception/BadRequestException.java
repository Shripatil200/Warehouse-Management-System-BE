package com.infotact.warehouse.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a client request fails business logic validation.
 * <p>
 * This exception maps to an <b>HTTP 400 Bad Request</b>. It is used for
 * scenarios where the request is malformed or violates business rules
 * (e.g., negative stock quantities, invalid date ranges, or mismatched IDs).
 * </p>
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

    /**
     * Constructs a new BadRequestException with a specific error message.
     * @param message The detail message explaining the business rule violation.
     */
    public BadRequestException(String message) {
        super(message);
    }
}