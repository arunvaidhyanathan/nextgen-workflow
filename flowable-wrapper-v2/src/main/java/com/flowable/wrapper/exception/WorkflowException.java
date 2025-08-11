package com.flowable.wrapper.exception;

/**
 * Base exception for workflow-related errors (checked exception)
 */
public class WorkflowException extends Exception {
    
    private final String errorCode;
    
    public WorkflowException(String message) {
        super(message);
        this.errorCode = "WORKFLOW_ERROR";
    }
    
    public WorkflowException(String message, Throwable cause) {
        super(message, cause);
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