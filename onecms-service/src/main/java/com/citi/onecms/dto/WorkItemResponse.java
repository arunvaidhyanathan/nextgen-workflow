package com.citi.onecms.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a work item in the system.
 */
public class WorkItemResponse {
  private String workItemId;
  private String caseId;
  private String caseNumber;
  private String caseTitle;
  private String casePriority;
  private String taskName;
  private String taskType;
  private Long taskKey;
  private String status;
  private LocalDate dueDate;
  private LocalDateTime createdAt;
  private LocalDateTime completedAt;
  private String assignedToName;
  private Long assignedToUserId;
  private String completedByName;

  /**
   * Default constructor.
   */
  public WorkItemResponse() {}

  /**
   * Constructor with parameters.
   */
  public WorkItemResponse(String workItemId, String taskName, String taskType, String status) {
    this.workItemId = workItemId;
    this.taskName = taskName;
    this.taskType = taskType;
    this.status = status;
  }

  public String getWorkItemId() {
    return workItemId;
  }

  public void setWorkItemId(String workItemId) {
    this.workItemId = workItemId;
  }

  public String getCaseId() {
    return caseId;
  }

  public void setCaseId(String caseId) {
    this.caseId = caseId;
  }

  public String getCaseNumber() {
    return caseNumber;
  }

  public void setCaseNumber(String caseNumber) {
    this.caseNumber = caseNumber;
  }

  public String getCaseTitle() {
    return caseTitle;
  }

  public void setCaseTitle(String caseTitle) {
    this.caseTitle = caseTitle;
  }

  public String getCasePriority() {
    return casePriority;
  }

  public void setCasePriority(String casePriority) {
    this.casePriority = casePriority;
  }

  public String getTaskName() {
    return taskName;
  }

  public void setTaskName(String taskName) {
    this.taskName = taskName;
  }

  public String getTaskType() {
    return taskType;
  }

  public void setTaskType(String taskType) {
    this.taskType = taskType;
  }

  public Long getTaskKey() {
    return taskKey;
  }

  public void setTaskKey(Long taskKey) {
    this.taskKey = taskKey;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public void setDueDate(LocalDate dueDate) {
    this.dueDate = dueDate;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(LocalDateTime completedAt) {
    this.completedAt = completedAt;
  }

  public String getAssignedToName() {
    return assignedToName;
  }

  public void setAssignedToName(String assignedToName) {
    this.assignedToName = assignedToName;
  }

  public Long getAssignedToUserId() {
    return assignedToUserId;
  }

  public void setAssignedToUserId(Long assignedToUserId) {
    this.assignedToUserId = assignedToUserId;
  }

  public String getCompletedByName() {
    return completedByName;
  }

  public void setCompletedByName(String completedByName) {
    this.completedByName = completedByName;
  }
}