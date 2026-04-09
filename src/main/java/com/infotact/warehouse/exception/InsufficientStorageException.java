package com.infotact.warehouse.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a 'Put-away' or 'Transfer' operation exceeds the physical
 * capacity of a storage unit.
 * <p>
 * This exception is typically triggered during stock placement logic if the
 * requested quantity plus the current occupancy exceeds the {@link com.infotact.warehouse.entity.StorageBin}
 * maximum capacity. It maps to an <b>HTTP 400 Bad Request</b>.
 * </p>
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InsufficientStorageException extends RuntimeException {

    /**
     * Constructs a new InsufficientStorageException with a specific error message.
     * @param message The detail message explaining the capacity breach
     * (e.g., "Bin {code} only has room for 5 units, but 10 were requested").
     */
    public InsufficientStorageException(String message) {
        super(message);
    }
}