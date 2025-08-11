package com.citi.onecms.exception;

/**
 * Exception thrown when user is not authorized to perform an action
 */
public class UnauthorizedException extends RuntimeException {

    private String action;
    private String resource;
    private String userId;

    /**
     * Constructor with message only
     */
    public UnauthorizedException(String message) {
        super(message);
    }

    /**
     * Constructor with message and cause
     */
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with action and resource details
     */
    public UnauthorizedException(String action, String resource) {
        super(String.format("Not authorized to %s %s", action, resource));
        this.action = action;
        this.resource = resource;
    }

    /**
     * Constructor with action, resource, and user details
     */
    public UnauthorizedException(String action, String resource, String userId) {
        super(String.format("User %s is not authorized to %s %s", userId, action, resource));
        this.action = action;
        this.resource = resource;
        this.userId = userId;
    }

    /**
     * Constructor with all details and cause
     */
    public UnauthorizedException(String action, String resource, String userId, Throwable cause) {
        super(String.format("User %s is not authorized to %s %s", userId, action, resource), cause);
        this.action = action;
        this.resource = resource;
        this.userId = userId;
    }

    // Getters
    public String getAction() {
        return action;
    }

    public String getResource() {
        return resource;
    }

    public String getUserId() {
        return userId;
    }
}