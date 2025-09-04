package com.citi.onecms.dto;

import com.citi.onecms.entity.Priority;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a case draft with workflow integration")
public class CreateCaseDraftRequest {
    
    @NotBlank(message = "Case draft title is required")
    @Size(max = 500, message = "Case draft title must not exceed 500 characters")
    @Schema(description = "Title of the case draft", example = "Employee Misconduct Investigation", required = true)
    @JsonProperty("case_draft_title")
    private String caseDraftTitle;
    
    @Schema(description = "Description of the case draft", example = "Detailed description of the investigation case")
    @JsonProperty("case_draft_description")
    private String caseDraftDescription;
    
    @Schema(description = "Priority of the case", example = "HIGH")
    @Builder.Default
    private Priority priority = Priority.MEDIUM;
    
    @Schema(description = "Case type ID", example = "1")
    @JsonProperty("case_type_id")
    private Long caseTypeId;
    
    @Schema(description = "Department ID", example = "1")
    @JsonProperty("department_id")
    private Long departmentId;
    
    // Escalation Channel Information (from UI screen)
    @NotNull(message = "Date received by escalation channel is required")
    @Schema(description = "Date when complaint was received by escalation channel", example = "2025-04-15", required = true)
    @JsonProperty("date_received_by_escalation_channel")
    private LocalDate dateReceivedByEscalationChannel;
    
    @NotBlank(message = "How complaint was received is required")
    @Size(max = 100, message = "Complaint received method must not exceed 100 characters")
    @Schema(description = "Method through which complaint was received", 
           example = "Citi Ethics Office", 
           allowableValues = {"Citi Ethics Office", "Employee Complaints Referrals", "Sales Practices", 
                             "Direct to EO", "Email", "Fax", "Hotline", "Phone"}, 
           required = true)
    @JsonProperty("complaint_received_method")
    private String complaintReceivedMethod;
    
    // Workflow Selection (optional - can be determined by business rules)
    @Schema(description = "Process definition key for workflow", 
           example = "oneCmsUnifiedWorkflow",
           allowableValues = {"oneCmsUnifiedWorkflow", "OneCMS_Workflow", "OneCMS_MultiDepartment_Workflow"})
    @JsonProperty("process_definition_key")
    private String processDefinitionKey;
    
    // Business Key Generation (optional - will be generated if not provided)
    @Schema(description = "Business key for the workflow instance", example = "DRAFT-2025-001")
    @JsonProperty("business_key")
    private String businessKey;
}