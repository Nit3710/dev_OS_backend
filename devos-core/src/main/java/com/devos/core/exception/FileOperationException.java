package com.devos.core.exception;

public class FileOperationException extends DevosException {
    
    public FileOperationException(String message) {
        super(message);
    }
    
    public FileOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
