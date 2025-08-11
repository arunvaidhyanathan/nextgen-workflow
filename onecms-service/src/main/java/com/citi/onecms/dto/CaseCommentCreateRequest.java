package com.citi.onecms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CaseCommentCreateRequest {
    
    @NotBlank(message = "Case ID is required")
    private String caseId;
    
    @NotBlank(message = "Comment text is required")
    @Size(max = 4000, message = "Comment text must be 4000 characters or less")
    private String commentText;
    
    private String commentType = "GENERAL";
    private Long parentCommentId;
    private Boolean isInternal = false;
    private Boolean isConfidential = false;
    
    // Getters and Setters
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    
    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }
    
    public String getCommentType() { return commentType; }
    public void setCommentType(String commentType) { this.commentType = commentType; }
    
    public Long getParentCommentId() { return parentCommentId; }
    public void setParentCommentId(Long parentCommentId) { this.parentCommentId = parentCommentId; }
    
    public Boolean getIsInternal() { return isInternal; }
    public void setIsInternal(Boolean isInternal) { this.isInternal = isInternal; }
    
    public Boolean getIsConfidential() { return isConfidential; }
    public void setIsConfidential(Boolean isConfidential) { this.isConfidential = isConfidential; }
}