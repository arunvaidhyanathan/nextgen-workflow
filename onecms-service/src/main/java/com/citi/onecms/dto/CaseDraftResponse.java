package com.citi.onecms.dto;

import com.citi.onecms.entity.CaseDraft;
import com.citi.onecms.entity.Priority;
import com.citi.onecms.enums.TaskStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing case draft details with workflow information")
public class CaseDraftResponse {
    
    @Schema(description = "Case ID (formatted case number for external identification)", example = "CMS-2025-000123")
    @JsonProperty("case_id")
    private String caseId;
    
    @Schema(description = "Case draft title", example = "Employee Misconduct Investigation")
    @JsonProperty("case_draft_title")
    private String caseDraftTitle;
    
    @Schema(description = "Case draft description", example = "Detailed description of the investigation case")
    @JsonProperty("case_draft_description")
    private String caseDraftDescription;
    
    @Schema(description = "Priority of the case", example = "HIGH")
    private Priority priority;
    
    @Schema(description = "Case type ID", example = "1")
    @JsonProperty("case_type_id")
    private Long caseTypeId;
    
    @Schema(description = "Case type name", example = "Investigation")
    @JsonProperty("case_type_name")
    private String caseTypeName;
    
    @Schema(description = "Department ID", example = "1")
    @JsonProperty("department_id")
    private Long departmentId;
    
    // Escalation Channel Information
    @Schema(description = "Date received by escalation channel", example = "2025-04-15")
    @JsonProperty("date_received_by_escalation_channel")
    private LocalDate dateReceivedByEscalationChannel;
    
    @Schema(description = "Method through which complaint was received", example = "Citi Ethics Office")
    @JsonProperty("complaint_received_method")
    private String complaintReceivedMethod;
    
    // Workflow Information
    @Schema(description = "Process instance ID from workflow engine", example = "proc_inst_456")
    @JsonProperty("process_instance_id")
    private String processInstanceId;
    
    @Schema(description = "Process definition key", example = "oneCmsUnifiedWorkflow")
    @JsonProperty("process_definition_key")
    private String processDefinitionKey;
    
    
    // Initial Task Information
    @Schema(description = "Initial task ID created", example = "task_789")
    @JsonProperty("initial_task_id")
    private String initialTaskId;
    
    @Schema(description = "Initial task name", example = "EO Head Review")
    @JsonProperty("initial_task_name")
    private String initialTaskName;
    
    @Schema(description = "Initial task queue", example = "eo-head-queue")
    @JsonProperty("initial_task_queue")
    private String initialTaskQueue;
    
    @Schema(description = "Candidate group for task assignment", example = "GROUP_EO_HEAD")
    @JsonProperty("candidate_group")
    private String candidateGroup;
    
    @Schema(description = "Current task status", example = "OPEN")
    @JsonProperty("task_status")
    private TaskStatus taskStatus;
    
    // Audit Information
    @Schema(description = "User who created the draft", example = "alice.intake")
    @JsonProperty("created_by_user_id")
    private String createdByUserId;
    
    @Schema(description = "When the draft was created", example = "2025-01-15T10:30:00")
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @Schema(description = "When the draft was last updated", example = "2025-01-15T10:30:00")
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    // Status Information
    @Schema(description = "Current status of the draft", example = "DRAFT")
    @JsonProperty("draft_status")
    private CaseDraft.DraftStatus draftStatus;
    
    @Schema(description = "Whether the draft has been submitted", example = "false")
    @JsonProperty("is_submitted")
    private Boolean isSubmitted;
    
    @Schema(description = "When the draft was submitted", example = "2025-01-15T11:00:00")
    @JsonProperty("submitted_at")
    private LocalDateTime submittedAt;
    
    // Workflow Status Information
    @Schema(description = "Current workflow status", example = "STARTED")
    @JsonProperty("workflow_status")
    private String workflowStatus;
    
    /**
     * Factory method to create response from entity
     */
    public static CaseDraftResponse from(CaseDraft caseDraft) {
        CaseDraftResponseBuilder builder = CaseDraftResponse.builder()
                .caseDraftTitle(caseDraft.getCaseDraftTitle())
                .caseDraftDescription(caseDraft.getCaseDraftDescription())
                .priority(caseDraft.getPriority())
                .caseTypeId(caseDraft.getCaseTypeId())
                .departmentId(caseDraft.getDepartmentId())
                .dateReceivedByEscalationChannel(caseDraft.getDateReceivedByEscalationChannel())
                .complaintReceivedMethod(caseDraft.getComplaintReceivedMethod())
                .processInstanceId(caseDraft.getProcessInstanceId())
                .processDefinitionKey(caseDraft.getProcessDefinitionKey())
                .initialTaskId(caseDraft.getInitialTaskId())
                .initialTaskName(caseDraft.getInitialTaskName())
                .initialTaskQueue(caseDraft.getInitialTaskQueue())
                .candidateGroup(caseDraft.getCandidateGroup())
                .taskStatus(caseDraft.getTaskStatus())
                .createdByUserId(caseDraft.getCreatedByUserId())
                .createdAt(caseDraft.getCreatedAt())
                .updatedAt(caseDraft.getUpdatedAt())
                .draftStatus(caseDraft.getDraftStatus())
                .isSubmitted(caseDraft.getIsSubmitted())
                .submittedAt(caseDraft.getSubmittedAt());
        
        // Use case number as the external case_id
        if (caseDraft.getCaseEntity() != null) {
            builder.caseId(caseDraft.getCaseEntity().getCaseNumber());
        }
        
        // Add case type information if available
        if (caseDraft.getCaseType() != null) {
            builder.caseTypeName(caseDraft.getCaseType().getTypeName());
        }
        
        return builder.build();
    }
}