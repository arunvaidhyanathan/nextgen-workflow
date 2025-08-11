package com.flowable.wrapper.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskQueueMapping implements Serializable {
    
    private String taskId;  // Task definition key from BPMN
    
    private String taskName;  // Human-readable task name
    
    private List<String> candidateGroups;  // Groups from BPMN
    
    private String queue;  // Queue assignment for this task
    
    private TaskMetadata metadata;  // Additional task metadata
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskMetadata {
        private String documentation;
        private String formKey;
        private Integer priority;
        private String category;
    }
}