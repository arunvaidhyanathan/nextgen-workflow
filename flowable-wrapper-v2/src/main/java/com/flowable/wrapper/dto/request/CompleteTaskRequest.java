package com.flowable.wrapper.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to complete a task")
public class CompleteTaskRequest {
    
    @Schema(description = "Variables to set when completing the task", 
            example = "{\"approved\": true, \"comments\": \"Looks good\"}")
    private Map<String, Object> variables;
    
    @Schema(description = "Optional comment for the completion", example = "Approved after review")
    private String comment;
}