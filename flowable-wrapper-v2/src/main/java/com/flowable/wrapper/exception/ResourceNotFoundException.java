package com.flowable.wrapper.exception;

/**
 * Exception thrown when a requested resource is not found
 */
public class ResourceNotFoundException extends WorkflowRuntimeException {
    
    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }
    
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super("RESOURCE_NOT_FOUND", String.format("%s not found with id: %s", resourceType, resourceId));
    }
}