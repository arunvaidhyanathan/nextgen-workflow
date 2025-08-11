package com.citi.onecms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

@Schema(description = "Request to transition a workflow task")
public class TaskTransitionRequest {
    
    @NotBlank(message = "Task ID is required")
    @Schema(description = "ID of the task to complete", example = "12345")
    private String taskId;
    
    @Schema(description = "Variables to pass when completing the task")
    private Map<String, Object> variables;
    
    @Schema(description = "Comments or notes for the task completion")
    private String comments;
    
    @Schema(description = "Decision or action taken", example = "approved")
    private String decision;
    
    // Constructors
    public TaskTransitionRequest() {}
    
    public TaskTransitionRequest(String taskId, Map<String, Object> variables) {
        this.taskId = taskId;
        this.variables = variables;
    }
    
    // Getters and Setters
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    
    public Map<String, Object> getVariables() { return variables; }
    public void setVariables(Map<String, Object> variables) { this.variables = variables; }
    
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
}