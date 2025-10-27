package com.citi.onecms.dto;

public class EmailCaseUploadResponse {
    private Long callId;
    private String status;
    private String message;
    
    public EmailCaseUploadResponse() {}
    
    public EmailCaseUploadResponse(Long callId, String status, String message) {
        this.callId = callId;
        this.status = status;
        this.message = message;
    }
    
    // Getters and Setters
    public Long getCallId() {
        return callId;
    }
    
    public void setCallId(Long callId) {
        this.callId = callId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}