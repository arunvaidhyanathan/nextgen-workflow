package com.citi.onecms.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartProcessWithTaskResponse {
    
    // Process information
    private String processInstanceId;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String businessKey;
    private boolean active;
    private boolean suspended;
    private Instant startTime;
    private Map<String, Object> variables;
    
    // Initial task information
    private TaskResponse initialTask;
    private List<TaskResponse> allInitialTasks;
    
    // Workflow metadata
    private String initiatedBy;
    private Instant createdAt;
    
    // Factory method to create from StartProcessResponse
    public static StartProcessWithTaskResponse from(StartProcessResponse processResponse) {
        return StartProcessWithTaskResponse.builder()
                .processInstanceId(processResponse.getProcessInstanceId())
                .processDefinitionId(processResponse.getProcessDefinitionId())
                .processDefinitionKey(processResponse.getProcessDefinitionKey())
                .businessKey(processResponse.getBusinessKey())
                .active(processResponse.isActive())
                .suspended(processResponse.isSuspended())
                .startTime(processResponse.getStartTime())
                .createdAt(Instant.now())
                .build();
    }
}