package com.citi.onecms.dto;   

import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

public class TaskCompleteRequest {
    @NotNull
    private Long taskKey;
    
    private Map<String, Object> variables = new HashMap<>();
    
    private String outcome;
    private String comments;
    
    // Getters and Setters
    public Long getTaskKey() { return taskKey; }
    public void setTaskKey(Long taskKey) { this.taskKey = taskKey; }
    
    public Map<String, Object> getVariables() { return variables; }
    public void setVariables(Map<String, Object> variables) { this.variables = variables; }
    
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { 
        this.outcome = outcome;
        this.variables.put("outcome", outcome);
    }
    
    public String getComments() { return comments; }
    public void setComments(String comments) { 
        this.comments = comments;
        this.variables.put("comments", comments);
    }
}