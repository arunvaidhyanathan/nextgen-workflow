package com.citi.onecms.dto;

import com.citi.onecms.entity.CaseStatus;
import com.citi.onecms.entity.Priority;
import com.citi.onecms.entity.Severity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents the complete case information with a list of associated allegations.
 */
@Schema(description = "Complete case information with allegations")
public class CaseWithAllegationsResponse {

  @Schema(description = "Case ID", example = "CMS-2024-001")
  private String caseId;

  @Schema(description = "Case number", example = "CMS-2024-001")
  private String caseNumber;

  @Schema(description = "Case title", example = "Employee Misconduct Investigation")
  private String title;

  @Schema(description = "Case description", example = "Investigation of multiple allegations")
  private String description;

  @Schema(description = "Case priority", example = "HIGH")
  private Priority priority;

  @Schema(description = "Case status", example = "OPEN")
  private CaseStatus status;

  @Schema(description = "Complainant name", example = "Jane Smith")
  private String complainantName;

  @Schema(description = "Complainant email", example = "jane.smith@company.com")
  private String complainantEmail;

  @Schema(description = "Workflow instance key")
  private Long workflowInstanceKey;

  @Schema(description = "Case creation timestamp")
  private LocalDateTime createdAt;

  @Schema(description = "Case last updated timestamp")
  private LocalDateTime updatedAt;

  @Schema(description = "User who created the case")
  private String createdBy;

  @Schema(description = "User assigned to the case")
  private String assignedTo;
  
  @Schema(description = "Process instance ID from workflow")
  private String processInstanceId;
  
  @Schema(description = "Initial task ID when case was created")
  private String initialTaskId;
  
  @Schema(description = "Current task ID")
  private String currentTaskId;

  @Schema(description = "List of allegations")
  private List<AllegationResponse> allegations;
  
  @Schema(description = "List of case entities (people/organizations)")
  private List<EntityResponse> entities;
  
  @Schema(description = "List of case narratives")
  private List<NarrativeResponse> narratives;
  
  @Schema(description = "Workflow metadata")
  private WorkflowMetadata workflowMetadata;

  /**
   * Represents the details of an allegation.
   */
  @Schema(description = "Allegation details")
  public static class AllegationResponse {

    @Schema(description = "Allegation ID", example = "ALG-2024-001")
    private String allegationId;

    @Schema(description = "Allegation type", example = "Sexual Harassment")
    private String allegationType;

    @Schema(description = "Severity level", example = "HIGH")
    private Severity severity;

    @Schema(description = "Allegation description")
    private String description;

    @Schema(description = "Department classification", example = "HR")
    private String departmentClassification;

    @Schema(description = "Assigned group", example = "HR_SPECIALIST")
    private String assignedGroup;

    @Schema(description = "Flowable plan item ID")
    private String flowablePlanItemId;

    @Schema(description = "Allegation creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Allegation last updated timestamp")
    private LocalDateTime updatedAt;

    /**
     * Default constructor.
     */
    public AllegationResponse() {}

    public String getAllegationId() {
      return allegationId;
    }

    public void setAllegationId(String allegationId) {
      this.allegationId = allegationId;
    }

    public String getAllegationType() {
      return allegationType;
    }

    public void setAllegationType(String allegationType) {
      this.allegationType = allegationType;
    }

    public Severity getSeverity() {
      return severity;
    }

    public void setSeverity(Severity severity) {
      this.severity = severity;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public String getDepartmentClassification() {
      return departmentClassification;
    }

    public void setDepartmentClassification(String departmentClassification) {
      this.departmentClassification = departmentClassification;
    }

    public String getAssignedGroup() {
      return assignedGroup;
    }

    public void setAssignedGroup(String assignedGroup) {
      this.assignedGroup = assignedGroup;
    }

    public String getFlowablePlanItemId() {
      return flowablePlanItemId;
    }

    public void setFlowablePlanItemId(String flowablePlanItemId) {
      this.flowablePlanItemId = flowablePlanItemId;
    }

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
      return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt;
    }
  }

  /**
   * Default constructor.
   */
  public CaseWithAllegationsResponse() {}

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

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Priority getPriority() {
    return priority;
  }

  public void setPriority(Priority priority) {
    this.priority = priority;
  }

  public CaseStatus getStatus() {
    return status;
  }

  public void setStatus(CaseStatus status) {
    this.status = status;
  }

  public String getComplainantName() {
    return complainantName;
  }

  public void setComplainantName(String complainantName) {
    this.complainantName = complainantName;
  }

  public String getComplainantEmail() {
    return complainantEmail;
  }

  public void setComplainantEmail(String complainantEmail) {
    this.complainantEmail = complainantEmail;
  }

  public Long getWorkflowInstanceKey() {
    return workflowInstanceKey;
  }

  public void setWorkflowInstanceKey(Long workflowInstanceKey) {
    this.workflowInstanceKey = workflowInstanceKey;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public String getAssignedTo() {
    return assignedTo;
  }

  public void setAssignedTo(String assignedTo) {
    this.assignedTo = assignedTo;
  }

  public List<AllegationResponse> getAllegations() {
    return allegations;
  }

  public void setAllegations(List<AllegationResponse> allegations) {
    this.allegations = allegations;
  }
  
  public String getProcessInstanceId() {
    return processInstanceId;
  }
  
  public void setProcessInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }
  
  public String getInitialTaskId() {
    return initialTaskId;
  }
  
  public void setInitialTaskId(String initialTaskId) {
    this.initialTaskId = initialTaskId;
  }
  
  public String getCurrentTaskId() {
    return currentTaskId;
  }
  
  public void setCurrentTaskId(String currentTaskId) {
    this.currentTaskId = currentTaskId;
  }
  
  public List<EntityResponse> getEntities() {
    return entities;
  }
  
  public void setEntities(List<EntityResponse> entities) {
    this.entities = entities;
  }
  
  public List<NarrativeResponse> getNarratives() {
    return narratives;
  }
  
  public void setNarratives(List<NarrativeResponse> narratives) {
    this.narratives = narratives;
  }
  
  public WorkflowMetadata getWorkflowMetadata() {
    return workflowMetadata;
  }
  
  public void setWorkflowMetadata(WorkflowMetadata workflowMetadata) {
    this.workflowMetadata = workflowMetadata;
  }
  
  /**
   * Represents case entity (person/organization) details in response
   */
  @Schema(description = "Case entity (person or organization) details")
  public static class EntityResponse {
    
    @Schema(description = "Entity ID")
    private String entityId;
    
    @Schema(description = "Entity type", example = "PERSON")
    private String entityType;
    
    @Schema(description = "Relationship type", example = "Subject")
    private String relationshipType;
    
    @Schema(description = "Display name for the entity")
    private String displayName;
    
    @Schema(description = "SOEID for person entities")
    private String soeid;
    
    @Schema(description = "Email address")
    private String emailAddress;
    
    @Schema(description = "Organization name for organization entities")
    private String organizationName;
    
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;
    
    // Constructors
    public EntityResponse() {}
    
    // Getters and Setters
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    
    public String getRelationshipType() { return relationshipType; }
    public void setRelationshipType(String relationshipType) { this.relationshipType = relationshipType; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getSoeid() { return soeid; }
    public void setSoeid(String soeid) { this.soeid = soeid; }
    
    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }
    
    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  }
  
  /**
   * Represents case narrative details in response
   */
  @Schema(description = "Case narrative details")
  public static class NarrativeResponse {
    
    @Schema(description = "Narrative ID")
    private String narrativeId;
    
    @Schema(description = "Narrative type", example = "Original Claim")
    private String narrativeType;
    
    @Schema(description = "Narrative title")
    private String narrativeTitle;
    
    @Schema(description = "Narrative text content")
    private String narrativeText;
    
    @Schema(description = "Investigation function")
    private String investigationFunction;
    
    @Schema(description = "Whether narrative is recalled")
    private Boolean isRecalled;
    
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;
    
    // Constructors
    public NarrativeResponse() {}
    
    // Getters and Setters
    public String getNarrativeId() { return narrativeId; }
    public void setNarrativeId(String narrativeId) { this.narrativeId = narrativeId; }
    
    public String getNarrativeType() { return narrativeType; }
    public void setNarrativeType(String narrativeType) { this.narrativeType = narrativeType; }
    
    public String getNarrativeTitle() { return narrativeTitle; }
    public void setNarrativeTitle(String narrativeTitle) { this.narrativeTitle = narrativeTitle; }
    
    public String getNarrativeText() { return narrativeText; }
    public void setNarrativeText(String narrativeText) { this.narrativeText = narrativeText; }
    
    public String getInvestigationFunction() { return investigationFunction; }
    public void setInvestigationFunction(String investigationFunction) { this.investigationFunction = investigationFunction; }
    
    public Boolean getIsRecalled() { return isRecalled; }
    public void setIsRecalled(Boolean isRecalled) { this.isRecalled = isRecalled; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  }
  
  /**
   * Represents workflow metadata in response
   */
  @Schema(description = "Workflow metadata for the case")
  public static class WorkflowMetadata {
    
    @Schema(description = "Process definition key")
    private String processDefinitionKey;
    
    @Schema(description = "Process instance ID")
    private String processInstanceId;
    
    @Schema(description = "Initial task information")
    private TaskInfo initialTask;
    
    @Schema(description = "Current task information")
    private TaskInfo currentTask;
    
    @Schema(description = "Workflow status", example = "STARTED")
    private String status;
    
    // Constructors
    public WorkflowMetadata() {}
    
    // Getters and Setters
    public String getProcessDefinitionKey() { return processDefinitionKey; }
    public void setProcessDefinitionKey(String processDefinitionKey) { this.processDefinitionKey = processDefinitionKey; }
    
    public String getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }
    
    public TaskInfo getInitialTask() { return initialTask; }
    public void setInitialTask(TaskInfo initialTask) { this.initialTask = initialTask; }
    
    public TaskInfo getCurrentTask() { return currentTask; }
    public void setCurrentTask(TaskInfo currentTask) { this.currentTask = currentTask; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    /**
     * Task information within workflow metadata
     */
    @Schema(description = "Task information")
    public static class TaskInfo {
      
      @Schema(description = "Task ID")
      private String taskId;
      
      @Schema(description = "Task name")
      private String taskName;
      
      @Schema(description = "Queue name")
      private String queueName;
      
      @Schema(description = "Task status")
      private String status;
      
      // Constructors
      public TaskInfo() {}
      
      // Getters and Setters
      public String getTaskId() { return taskId; }
      public void setTaskId(String taskId) { this.taskId = taskId; }
      
      public String getTaskName() { return taskName; }
      public void setTaskName(String taskName) { this.taskName = taskName; }
      
      public String getQueueName() { return queueName; }
      public void setQueueName(String queueName) { this.queueName = queueName; }
      
      public String getStatus() { return status; }
      public void setStatus(String status) { this.status = status; }
    }
  }
}