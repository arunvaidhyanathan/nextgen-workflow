package com.citi.onecms.dto.workflow;

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
public class TaskResponse {
    private String taskId;
    private String processInstanceId;
    private String processDefinitionKey;
    private String taskDefinitionKey;
    private String taskName;
    private String queueName;
    private String assignee;
    private String status;
    private Integer priority;
    private Instant createdAt;
    private Instant claimedAt;
    private Instant completedAt;
    private Map<String, Object> taskData;
}