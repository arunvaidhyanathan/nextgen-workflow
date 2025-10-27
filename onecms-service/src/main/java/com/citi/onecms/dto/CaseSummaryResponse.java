package com.citi.onecms.dto;

import java.time.LocalDateTime;

public class CaseSummaryResponse {
    private Long caseSummaryId;
    private Long caseId;
    private String statusId;
    private String summaryText;
    private LocalDateTime createdAt;
    
    public CaseSummaryResponse() {}
    
    public CaseSummaryResponse(Long caseSummaryId, Long caseId, String statusId, 
                              String summaryText, LocalDateTime createdAt) {
        this.caseSummaryId = caseSummaryId;
        this.caseId = caseId;
        this.statusId = statusId;
        this.summaryText = summaryText;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public Long getCaseSummaryId() {
        return caseSummaryId;
    }
    
    public void setCaseSummaryId(Long caseSummaryId) {
        this.caseSummaryId = caseSummaryId;
    }
    
    public Long getCaseId() {
        return caseId;
    }
    
    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }
    
    public String getStatusId() {
        return statusId;
    }
    
    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }
    
    public String getSummaryText() {
        return summaryText;
    }
    
    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}