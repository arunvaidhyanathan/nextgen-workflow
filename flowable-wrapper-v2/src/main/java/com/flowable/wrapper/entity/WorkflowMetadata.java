package com.flowable.wrapper.entity;

import com.flowable.wrapper.model.TaskQueueMapping;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "workflow_metadata")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowMetadata {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "process_definition_key", nullable = false, unique = true)
    private String processDefinitionKey;  // This should match the BPMN process id
    
    @Column(name = "process_name", nullable = false)
    private String processName;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;
    
    @Column(name = "business_app_name", nullable = false)
    private String businessAppName;  // Reference to business application by name instead of FK
    
    @Type(JsonType.class)
    @Column(name = "candidate_group_mappings", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> candidateGroupMappings;  // candidateGroup -> queueName
    
    @Type(JsonType.class)
    @Column(name = "task_queue_mappings", columnDefinition = "jsonb")
    private List<TaskQueueMapping> taskQueueMappings;  // Populated after deployment
    
    @Type(JsonType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @Column(name = "deployed", nullable = false)
    @Builder.Default
    private Boolean deployed = false;
    
    @Column(name = "deployment_id")
    private String deploymentId;
    
    @Column(name = "created_by", nullable = false)
    @Builder.Default
    private String createdBy = "system";
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}