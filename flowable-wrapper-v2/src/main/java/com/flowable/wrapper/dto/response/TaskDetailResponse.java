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
@Schema(description = "Detailed task information including form data")
public class TaskDetailResponse {
    
    @Schema(description = "Task ID", example = "5d7e9c3a-1234-5678-9abc-def012345678")
    private String taskId;
    
    @Schema(description = "Process instance ID", example = "5d7e9c3a-1234-5678-9abc-def012345678")
    private String processInstanceId;
    
    @Schema(description = "Process definition key", example = "simpleApproval")
    private String processDefinitionKey;
    
    @Schema(description = "Task definition key", example = "managerApproval")
    private String taskDefinitionKey;
    
    @Schema(description = "Task name", example = "Manager Approval")
    private String taskName;
    
    @Schema(description = "Queue name", example = "default")
    private String queueName;
    
    @Schema(description = "Assignee user ID", example = "john.doe")
    private String assignee;
    
    @Schema(description = "Task status", example = "OPEN", allowableValues = {"OPEN", "CLAIMED", "COMPLETED"})
    private String status;
    
    @Schema(description = "Task priority", example = "50")
    private Integer priority;
    
    @Schema(description = "Task creation time")
    private Instant createdAt;
    
    @Schema(description = "Task claim time")
    private Instant claimedAt;
    
    @Schema(description = "Additional task data")
    private Map<String, Object> taskData;
    
    @Schema(description = "Business key of the process", example = "ORDER-2024-001")
    private String businessKey;
    
    @Schema(description = "Form key for the task", example = "approvalForm")
    private String formKey;
    
    @Schema(description = "Form data/variables for the task")
    private Map<String, Object> formData;
    
    @Schema(description = "Form properties definition from BPMN")
    private Map<String, Object> formProperties;
    
    @Schema(description = "Process variables")
    private Map<String, Object> processVariables;
    
    @Schema(description = "Task description")
    private String description;
    
    @Schema(description = "Task due date")
    private Instant dueDate;
}