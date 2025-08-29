package com.citi.onecms.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Contains the result of starting a workflow process including process and task information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStartResult {
    
    private String processInstanceId;
    private String processDefinitionKey;
    private String businessKey;
    private String initialTaskId;
    private String currentTaskId;
    private String status;
    private List<TaskResponse> allInitialTasks;
    
    // Utility methods
    public boolean isSuccessful() {
        return processInstanceId != null && !"FAILED".equals(status);
    }
    
    public boolean hasInitialTasks() {
        return allInitialTasks != null && !allInitialTasks.isEmpty();
    }
    
    public TaskResponse getFirstTask() {
        return hasInitialTasks() ? allInitialTasks.get(0) : null;
    }
}