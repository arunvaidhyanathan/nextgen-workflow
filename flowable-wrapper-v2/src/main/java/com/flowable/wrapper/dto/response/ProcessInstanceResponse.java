package com.flowable.wrapper.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Process instance details")
public class ProcessInstanceResponse {
    
    @Schema(description = "Process instance ID", example = "5d7e9c3a-1234-5678-9abc-def012345678")
    private String processInstanceId;
    
    @Schema(description = "Process definition ID", example = "simpleApproval:1:abc123")
    private String processDefinitionId;
    
    @Schema(description = "Process definition key", example = "simpleApproval")
    private String processDefinitionKey;
    
    @Schema(description = "Process definition name", example = "Simple Approval Process")
    private String processDefinitionName;
    
    @Schema(description = "Business key", example = "ORDER-2024-001")
    private String businessKey;
    
    @Schema(description = "Start time of the process instance")
    private Instant startTime;
    
    @Schema(description = "Started by user", example = "john.doe")
    private String startedBy;
    
    @Schema(description = "Is the process instance suspended", example = "false")
    private boolean suspended;
    
    @Schema(description = "Process variables")
    private Map<String, Object> variables;
    
    @Schema(description = "Is the process instance active (not completed)", example = "true")
    private Boolean active;
    
    @Schema(description = "End time of the process instance (for completed processes)")
    private Instant endTime;
    
    @Schema(description = "Duration in milliseconds (for completed processes)", example = "120000")
    private Long durationInMillis;
}