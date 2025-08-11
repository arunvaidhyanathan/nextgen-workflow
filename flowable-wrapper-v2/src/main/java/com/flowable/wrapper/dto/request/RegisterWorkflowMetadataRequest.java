package com.flowable.wrapper.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to register workflow metadata with queue mappings")
public class RegisterWorkflowMetadataRequest {
    
    @NotBlank(message = "Process definition key is required")
    @Schema(description = "Process definition key (must match BPMN process id)", example = "simpleApproval")
    private String processDefinitionKey;
    
    @NotBlank(message = "Process name is required")
    @Schema(description = "Human-readable name for the workflow", example = "Simple Approval Process")
    private String processName;
    
    @NotBlank(message = "Business application name is required")
    @Schema(description = "Business application this workflow belongs to", example = "Sanctions-Management")
    private String businessAppName;
    
    @Schema(description = "Description of the workflow", example = "Basic approval workflow for testing")
    private String description;
    
    @NotEmpty(message = "Candidate group mappings are required")
    @Schema(description = "Mapping of candidate groups to queue names", 
            example = "{\"managers\": \"default\", \"finance\": \"finance-queue\"}")
    private Map<String, String> candidateGroupMappings;
    
    @Schema(description = "Additional metadata for the workflow")
    private Map<String, Object> metadata;
}