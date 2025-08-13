package com.citi.onecms.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterWorkflowRequest {
    private String processDefinitionKey;
    private String processName;
    private String businessAppName;
    private Map<String, String> candidateGroupMappings;
    private Map<String, Object> taskQueueMappings;
    private boolean active;
    private String description;
    private Integer version;
}