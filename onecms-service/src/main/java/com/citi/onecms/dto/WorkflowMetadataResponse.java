package com.citi.onecms.dto;

import com.citi.onecms.util.TaskQueueMapping;
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
public class WorkflowMetadataResponse {
    
    private Long id;
    private String processDefinitionKey;
    private String processName;
    private String description;
    private Integer version;
    private Map<String, String> candidateGroupMappings;
    private List<TaskQueueMapping> taskQueueMappings;
    private Map<String, Object> metadata;
    private Boolean active;
    private Boolean deployed;
    private String deploymentId;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}