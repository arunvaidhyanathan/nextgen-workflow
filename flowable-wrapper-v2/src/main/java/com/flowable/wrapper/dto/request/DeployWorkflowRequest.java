package com.flowable.wrapper.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to deploy a BPMN workflow")
public class DeployWorkflowRequest {
    
    @NotBlank(message = "Process definition key is required")
    @Schema(description = "The process definition key registered in metadata", example = "simpleApproval")
    private String processDefinitionKey;
    
    @NotBlank(message = "BPMN XML content is required")
    @Schema(description = "The BPMN 2.0 XML content", example = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>...")
    private String bpmnXml;
    
    @Schema(description = "Optional deployment name", example = "Purchase Order v1.0")
    private String deploymentName;
}