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

  @Schema(description = "List of allegations")
  private List<AllegationResponse> allegations;

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
}