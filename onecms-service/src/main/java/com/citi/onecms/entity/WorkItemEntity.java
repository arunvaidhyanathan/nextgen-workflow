package com.citi.onecms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "work_items", schema = "onecms")
public class WorkItemEntity {
    
    @Id
    @Column(name = "work_item_id")
    private String workItemId;
    
    @NotBlank
    @Size(max = 100)
    @Column(name = "work_item_number", unique = true, nullable = false)
    private String workItemNumber;
    
    @NotBlank
    @Size(max = 100)
    @Column(name = "type")
    private String type;
    
    @NotBlank
    @Size(max = 50)
    @Column(name = "severity")
    private String severity;
    
    @Column(name = "description")
    private String description;
    
    @NotBlank
    @Size(max = 50)
    @Column(name = "status")
    private String status = "OPEN";
    
    @Size(max = 100)
    @Column(name = "classification")
    private String classification;
    
    @Size(max = 100)
    @Column(name = "assigned_group")
    private String assignedGroup;
    
    @Size(max = 50)
    @Column(name = "priority")
    private String priority;
    
    @Size(max = 255)
    @Column(name = "flowable_process_instance_id")
    private String flowableProcessInstanceId;
    
    @NotBlank
    @Size(max = 50)
    @Column(name = "case_id")
    private String caseId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public WorkItemEntity() {}
    
    public WorkItemEntity(String workItemId, String workItemNumber, String type, String severity, String description) {
        this.workItemId = workItemId;
        this.workItemNumber = workItemNumber;
        this.type = type;
        this.severity = severity;
        this.description = description;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Lifecycle Callbacks
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getWorkItemId() { return workItemId; }
    public void setWorkItemId(String workItemId) { this.workItemId = workItemId; }
    
    public String getWorkItemNumber() { return workItemNumber; }
    public void setWorkItemNumber(String workItemNumber) { this.workItemNumber = workItemNumber; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getClassification() { return classification; }
    public void setClassification(String classification) { this.classification = classification; }
    
    public String getAssignedGroup() { return assignedGroup; }
    public void setAssignedGroup(String assignedGroup) { this.assignedGroup = assignedGroup; }
    
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    
    public String getFlowableProcessInstanceId() { return flowableProcessInstanceId; }
    public void setFlowableProcessInstanceId(String flowableProcessInstanceId) { 
        this.flowableProcessInstanceId = flowableProcessInstanceId; 
    }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
}