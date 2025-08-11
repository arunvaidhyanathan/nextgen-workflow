package com.citi.onecms.exception;

public class WorkflowTaskNotFoundException extends RuntimeException {
    public WorkflowTaskNotFoundException(String message) {
        super(message);
    }
    
    public WorkflowTaskNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}