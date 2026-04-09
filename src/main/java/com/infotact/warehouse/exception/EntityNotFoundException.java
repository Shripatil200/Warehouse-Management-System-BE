package com.infotact.warehouse.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a requested resource does not exist in the database.
 * <p>
 * This maps to an <b>HTTP 404 Not Found</b> response. It is commonly used
 * in service layer 'findById' calls to ensure that downstream logic is not
 * performed on null objects.
 * </p>
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class EntityNotFoundException extends RuntimeException {

    /**
     * Constructs a new EntityNotFoundException with a specific error message.
     * @param message The detail message explaining which entity was missing
     * (e.g., "Product with ID {id} not found").
     */
    public EntityNotFoundException(String message){
        super(message);
    }
}