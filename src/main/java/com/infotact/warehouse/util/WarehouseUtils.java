package com.infotact.warehouse.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class WarehouseUtils {

    public WarehouseUtils() {
    }

    public static ResponseEntity<String> getResponseEntity(String responseMessage, HttpStatus httpStatus){
        return new ResponseEntity<String>("{\"message\":\""+responseMessage+"\"}", httpStatus);
    }
}
