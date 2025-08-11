package com.citi.onecms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;


@Entity
@Table(name = "case_transitions",  schema = "onecms")
public class CaseTransition {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "transition_id", unique = true, nullable = false)
    private Long transitionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", referencedColumnName = "id")
    private Case caseEntity;
    
    @Column(name = "from_status")
    @Enumerated(EnumType.STRING)
    private CaseStatus fromStatus;
    
    @Column(name = "to_status")
    @Enumerated(EnumType.STRING)
    private CaseStatus toStatus;
    
    @Column(name = "task_definition_key")
    private String taskDefinitionKey;
    
    @Column(name = "task_name")
    private String taskName;
    
    @Column(name = "workflow_instance_key")
    private Long workflowInstanceKey;
    
    @Column(name = "task_key")
    private Long taskKey;
    
    @Column(name = "performed_by_user_id", length = 100)
    private String performedByUserId;
    
    @Column(name = "transition_date")
    private LocalDateTime transitionDate;
    
    @Column(columnDefinition = "TEXT")
    private String comments;
    
    @ElementCollection
    @CollectionTable(name = "transition_variables")
    @MapKeyColumn(name = "variable_name")
    @Column(name = "variable_value")
    private Map<String, String> variables;
    
    // Constructors
    public CaseTransition() {}
    
    public CaseTransition(Case caseEntity, CaseStatus fromStatus, CaseStatus toStatus, 
                         String taskName, String performedByUserId) {
        this.caseEntity = caseEntity;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.taskName = taskName;
        this.performedByUserId = performedByUserId;
        this.transitionDate = LocalDateTime.now();
    }
    
    // Lifecycle Callbacks
    @PrePersist
    protected void onCreate() {
        if (transitionDate == null) {
            transitionDate = LocalDateTime.now();
        }
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getTransitionId() { return transitionId; }
    public void setTransitionId(Long transitionId) { this.transitionId = transitionId; }
    
    public Case getCaseEntity() { return caseEntity; }
    public void setCaseEntity(Case caseEntity) { this.caseEntity = caseEntity; }
    
    public CaseStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(CaseStatus fromStatus) { this.fromStatus = fromStatus; }
    
    public CaseStatus getToStatus() { return toStatus; }
    public void setToStatus(CaseStatus toStatus) { this.toStatus = toStatus; }
    
    public String getTaskDefinitionKey() { return taskDefinitionKey; }
    public void setTaskDefinitionKey(String taskDefinitionKey) { this.taskDefinitionKey = taskDefinitionKey; }
    
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    
    public Long getWorkflowInstanceKey() { return workflowInstanceKey; }
    public void setWorkflowInstanceKey(Long workflowInstanceKey) { this.workflowInstanceKey = workflowInstanceKey; }
    
    public Long getTaskKey() { return taskKey; }
    public void setTaskKey(Long taskKey) { this.taskKey = taskKey; }
    
    public String getPerformedByUserId() { return performedByUserId; }
    public void setPerformedByUserId(String performedByUserId) { this.performedByUserId = performedByUserId; }
    
    public LocalDateTime getTransitionDate() { return transitionDate; }
    public void setTransitionDate(LocalDateTime transitionDate) { this.transitionDate = transitionDate; }
    
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    
    public Map<String, String> getVariables() { return variables; }
    public void setVariables(Map<String, String> variables) { this.variables = variables; }

}