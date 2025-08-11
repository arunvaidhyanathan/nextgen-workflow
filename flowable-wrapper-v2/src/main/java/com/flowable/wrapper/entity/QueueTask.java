package com.flowable.wrapper.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowable.wrapper.enums.TaskStatus;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "queue_tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "taskId")
@ToString(exclude = "taskData")
public class QueueTask {
    
    @Id
    @Column(name = "task_id")
    private String taskId;
    
    @Column(name = "process_instance_id", nullable = false)
    private String processInstanceId;
    
    @Column(name = "process_definition_key", nullable = false)
    private String processDefinitionKey;
    
    @Column(name = "task_definition_key", nullable = false)
    private String taskDefinitionKey;
    
    @Column(name = "task_name", nullable = false)
    private String taskName;
    
    @Column(name = "queue_name", nullable = false)
    private String queueName;
    
    @Column(name = "assignee")
    private String assignee;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TaskStatus status = TaskStatus.OPEN;
    
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 50;
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @Column(name = "claimed_at")
    private Instant claimedAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    @Type(JsonBinaryType.class)
    @Column(name = "task_data", columnDefinition = "jsonb")
    private Map<String, Object> taskData;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = TaskStatus.OPEN;
        }
        if (priority == null) {
            priority = 50;
        }
    }
    
    @JsonIgnore
    public boolean isOpen() {
        return TaskStatus.OPEN.equals(status);
    }
    
    @JsonIgnore
    public boolean isClaimed() {
        return TaskStatus.CLAIMED.equals(status);
    }
    
    @JsonIgnore
    public boolean isCompleted() {
        return TaskStatus.COMPLETED.equals(status);
    }
}