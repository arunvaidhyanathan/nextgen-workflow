package com.citi.onecms.exception;

public class AllegationNotFoundException extends RuntimeException {
    public AllegationNotFoundException(String message) {
        super(message);
    }
    
    public AllegationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}