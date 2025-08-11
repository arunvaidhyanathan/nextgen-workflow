package com.citi.onecms.exception;

public class WorkflowException extends RuntimeException {
    
    private final String errorCode;
    
    public WorkflowException(String message) {
        super(message);
        this.errorCode = "WORKFLOW_ERROR";
    }
    
    public WorkflowException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public WorkflowException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}