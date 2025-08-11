package com.flowable.wrapper.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to start a new process instance")
public class StartProcessRequest {
    
    @NotBlank(message = "Process definition key is required")
    @Schema(description = "The process definition key", example = "simpleApproval", required = true)
    private String processDefinitionKey;
    
    @Schema(description = "Business key for the process instance", example = "ORDER-2024-001")
    private String businessKey;
    
    @Schema(description = "Process variables", example = "{\"requestor\": \"john.doe\", \"amount\": 1000}")
    private Map<String, Object> variables;
}