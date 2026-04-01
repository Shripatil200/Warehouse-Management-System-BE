package com.infotact.warehouse.exception;

public class InsufficientStorageException extends RuntimeException{
    public InsufficientStorageException(String message){
        super(message);
    }
}
