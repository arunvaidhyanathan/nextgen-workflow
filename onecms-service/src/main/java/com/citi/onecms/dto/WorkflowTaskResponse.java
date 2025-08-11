package com.citi.onecms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

/**
 * Represents a workflow task in the system.
 */
@Schema(description = "Workflow task information")
public class WorkflowTaskResponse {

  @Schema(description = "Task ID", example = "12345")
  private String taskId;

  @Schema(description = "Task name", example = "EO Intake - Intake Process")
  private String taskName;

  @Schema(description = "Task description")
  private String description;

  @Schema(description = "Process instance ID", example = "proc-inst-001")
  private String processInstanceId;

  @Schema(description = "Process definition ID")
  private String processDefinitionId;

  @Schema(description = "Task assignee", example = "john.doe")
  private String assignee;

  @Schema(description = "Candidate groups for the task")
  private String candidateGroups;

  @Schema(description = "Task definition key")
  private String taskDefinitionKey;

  @Schema(description = "Task creation time")
  private LocalDateTime created;

  @Schema(description = "Task due date")
  private LocalDateTime dueDate;

  @Schema(description = "Task priority", example = "50")
  private Integer priority;

  @Schema(description = "Task variables")
  private Map<String, Object> variables;

  @Schema(description = "Form key for the task")
  private String formKey;

  @Schema(description = "Associated case ID")
  private String caseId;

  @Schema(description = "Task category or type")
  private String category;

  /**
   * Default constructor.
   */
  public WorkflowTaskResponse() {}

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public String getTaskName() {
    return taskName;
  }

  public void setTaskName(String taskName) {
    this.taskName = taskName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getProcessInstanceId() {
    return processInstanceId;
  }

  public void setProcessInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  public String getProcessDefinitionId() {
    return processDefinitionId;
  }

  public void setProcessDefinitionId(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
  }

  public String getAssignee() {
    return assignee;
  }

  public void setAssignee(String assignee) {
    this.assignee = assignee;
  }

  public String getCandidateGroups() {
    return candidateGroups;
  }

  public void setCandidateGroups(String candidateGroups) {
    this.candidateGroups = candidateGroups;
  }

  public String getTaskDefinitionKey() {
    return taskDefinitionKey;
  }

  public void setTaskDefinitionKey(String taskDefinitionKey) {
    this.taskDefinitionKey = taskDefinitionKey;
  }

  public LocalDateTime getCreated() {
    return created;
  }

  public void setCreated(LocalDateTime created) {
    this.created = created;
  }

  public void setCreated(Date created) {
    this.created = created != null ? created.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null;
  }

  public LocalDateTime getDueDate() {
    return dueDate;
  }

  public void setDueDate(LocalDateTime dueDate) {
    this.dueDate = dueDate;
  }

  public void setDueDate(Date dueDate) {
    this.dueDate = dueDate != null ? dueDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null;
  }

  public Integer getPriority() {
    return priority;
  }

  public void setPriority(Integer priority) {
    this.priority = priority;
  }

  public Map<String, Object> getVariables() {
    return variables;
  }

  public void setVariables(Map<String, Object> variables) {
    this.variables = variables;
  }

  public String getFormKey() {
    return formKey;
  }

  public void setFormKey(String formKey) {
    this.formKey = formKey;
  }

  public String getCaseId() {
    return caseId;
  }

  public void setCaseId(String caseId) {
    this.caseId = caseId;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }
}