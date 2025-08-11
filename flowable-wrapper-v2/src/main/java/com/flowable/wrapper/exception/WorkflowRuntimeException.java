package com.flowable.wrapper.exception;

/**
 * Runtime exception for workflow-related errors (unchecked exception)
 */
public class WorkflowRuntimeException extends RuntimeException {
    
    private final String errorCode;
    
    public WorkflowRuntimeException(String message) {
        super(message);
        this.errorCode = "WORKFLOW_RUNTIME_ERROR";
    }
    
    public WorkflowRuntimeException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "WORKFLOW_RUNTIME_ERROR";
    }
    
    public WorkflowRuntimeException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public WorkflowRuntimeException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}