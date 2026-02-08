package com.devos.core.exception;

public class DevosException extends RuntimeException {
    
    public DevosException(String message) {
        super(message);
    }
    
    public DevosException(String message, Throwable cause) {
        super(message, cause);
    }
}
