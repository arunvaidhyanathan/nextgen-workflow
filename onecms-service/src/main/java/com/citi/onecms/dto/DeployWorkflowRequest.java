package com.citi.onecms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeployWorkflowRequest {
    
    @NotBlank(message = "Process definition key is required")
    private String processDefinitionKey;
    
    @NotBlank(message = "BPMN XML is required")
    private String bpmnXml;
    
    private String deploymentName;
}