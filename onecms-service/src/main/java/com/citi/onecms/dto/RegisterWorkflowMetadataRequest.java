package com.citi.onecms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterWorkflowMetadataRequest {
    
    @NotBlank(message = "Process definition key is required")
    private String processDefinitionKey;
    
    @NotBlank(message = "Process name is required")
    private String processName;
    
    private String description;
    
    @NotNull(message = "Candidate group mappings are required")
    private Map<String, String> candidateGroupMappings;  // candidateGroup -> queueName
    
    private Map<String, Object> metadata;
    
    private String createdBy;
}