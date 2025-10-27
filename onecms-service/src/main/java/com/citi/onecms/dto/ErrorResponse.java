package com.citi.onecms.dto;

public class ErrorResponse {
    private String error;
    private String message;
    
    // Constructor for backward compatibility
    public ErrorResponse(String message) { 
        this.error = "ERROR";
        this.message = message; 
    }
    
    // Constructor with error type and message
    public ErrorResponse(String error, String message) { 
        this.error = error;
        this.message = message; 
    }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}