package com.citi.onecms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity
@Table(name = "work_items",  schema = "onecms")
public class WorkItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "work_item_id", unique = true, nullable = false, length = 20)
    private String workItemId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", referencedColumnName = "id")
    private Case caseEntity;
    
    @Column(name = "assigned_to_user_id", length = 100)
    private String assignedToUserId;
    
    @NotBlank
    @Size(max = 200)
    @Column(name = "task_name")
    private String taskName;
    
    @NotBlank
    @Size(max = 100)
    @Column(name = "task_type")
    private String taskType;
    
    @Column(name = "task_key")
    private Long taskKey;
    
    @Column(name = "status")
    private String status = "PENDING";
    
    @Column(name = "due_date")
    private LocalDate dueDate;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "completed_by_user_id", length = 100)
    private String completedByUserId;
    
    // Constructors
    public WorkItem() {}
    
    public WorkItem(Case caseEntity, String taskName, String taskType, String assignedToUserId) {
        this.caseEntity = caseEntity;
        this.taskName = taskName;
        this.taskType = taskType;
        this.assignedToUserId = assignedToUserId;
        this.createdAt = LocalDateTime.now();
    }
    
    // Lifecycle Callbacks
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (workItemId == null) {
            // This will be set by the service layer using database sequence
        }
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getWorkItemId() { return workItemId; }
    public void setWorkItemId(String workItemId) { this.workItemId = workItemId; }
    
    public Case getCaseEntity() { return caseEntity; }
    
    public void setCaseEntity(Case caseEntity) {
        this.caseEntity = caseEntity;
    }
    
    public String getAssignedToUserId() { return assignedToUserId; }
    public void setAssignedToUserId(String assignedToUserId) { this.assignedToUserId = assignedToUserId; }
    
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    
    public Long getTaskKey() { return taskKey; }
    public void setTaskKey(Long taskKey) { this.taskKey = taskKey; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public String getCompletedByUserId() { return completedByUserId; }
    public void setCompletedByUserId(String completedByUserId) { this.completedByUserId = completedByUserId; }
}