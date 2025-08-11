package com.flowable.wrapper.dto.response;

import com.flowable.wrapper.model.TaskQueueMapping;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Response containing workflow metadata details")
public class WorkflowMetadataResponse {
    
    @Schema(description = "Unique identifier")
    private Long id;
    
    @Schema(description = "Process definition key")
    private String processDefinitionKey;
    
    @Schema(description = "Process name")
    private String processName;
    
    @Schema(description = "Description")
    private String description;
    
    @Schema(description = "Version number")
    private Integer version;
    
    @Schema(description = "Business application name")
    private String businessAppName;
    
    @Schema(description = "Candidate group to queue mappings")
    private Map<String, String> candidateGroupMappings;
    
    @Schema(description = "Task to queue mappings (populated after deployment)")
    private List<TaskQueueMapping> taskQueueMappings;
    
    @Schema(description = "Additional metadata")
    private Map<String, Object> metadata;
    
    @Schema(description = "Whether the workflow is active")
    private Boolean active;
    
    @Schema(description = "Created by user")
    private String createdBy;
    
    @Schema(description = "Creation timestamp")
    private Instant createdAt;
    
    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
    
    @Schema(description = "Whether the workflow has been deployed to Flowable")
    private Boolean deployed;
    
    @Schema(description = "Deployment ID from Flowable if deployed")
    private String deploymentId;
}