package com.infotact.warehouse.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a requested action violates specific business state rules.
 * <p>
 * This maps to an <b>HTTP 400 Bad Request</b>. It is used for operations that are
 * syntactically correct but logically prohibited due to the current state of
 * the system (e.g., attempting to delete a facility with active stock or
 * modifying a finalized purchase order).
 * </p>
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class IllegalOperationException extends RuntimeException {

    /**
     * Constructs a new IllegalOperationException with a specific error message.
     * @param message The detail message explaining why the operation is prohibited
     * in the current context.
     */
    public IllegalOperationException(String message) {
        super(message);
    }
}