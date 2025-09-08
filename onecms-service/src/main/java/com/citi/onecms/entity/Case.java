package com.citi.onecms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cases", schema = "onecms")
public class Case {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    // For compatibility with existing code that expects String caseId
    public String getCaseId() { 
        return id != null ? id.toString() : null; 
    }
    
    // For compatibility - setter that accepts String but converts to Long
    public void setCaseId(String caseId) {
        this.id = caseId != null ? Long.parseLong(caseId) : null;
    }
    
    @NotBlank
    @Size(max = 50)
    @Column(name = "case_number", unique = true)
    private String caseNumber;
    
    @NotBlank
    @Size(max = 255)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_type_id", referencedColumnName = "id")
    private CaseType caseType;
    
    @Column(name = "department_id")
    private Long departmentId;
    
    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.MEDIUM;
    
    @Enumerated(EnumType.STRING)
    private CaseStatus status = CaseStatus.OPEN;
    
    @Column(name = "assigned_to", length = 255)
    private String assignedToUserId;
    
    @Size(max = 200)
    @Column(name = "complainant_name")
    private String complainantName;
    
    @Size(max = 255)
    @Column(name = "complainant_email")
    private String complainantEmail;
    
    @Column(name = "workflow_instance_key")
    private Long workflowInstanceKey;
    
    @Column(name = "process_instance_id")
    private String processInstanceId;
    
    // Task IDs are managed by workflow service, not stored in case table
    // @Column(name = "initial_task_id")
    // private String initialTaskId;
    
    // @Column(name = "current_task_id")
    // private String currentTaskId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 255)
    private String createdByUserId;
    
    // Entity relationships - re-enabled with proper foreign key mappings
    @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorkItem> workItems = new ArrayList<>();
    
    @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<CaseTransition> transitions = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "case_id")
    private List<Allegation> allegations = new ArrayList<>();

    // Additional case fields from frontend requirements
    @Column(name = "occurrence_date")
    private LocalDate occurrenceDate;

    @Column(name = "date_reported_to_citi")
    private LocalDate dateReportedToCiti;

    @Column(name = "date_received_by_escalation_channel")
    private LocalDate dateReceivedByEscalationChannel;

    @Column(name = "complaint_escalated_by", length = 50)
    private String complaintEscalatedBy;

    @Column(name = "data_source_id", length = 50)
    private String dataSourceId;

    @Column(name = "cluster_country", length = 50)
    private String clusterCountry;

    @Column(name = "legal_hold")
    private Boolean legalHold = false;

    @Column(name = "outside_counsel")
    private Boolean outsideCounsel = false;

    @Column(name = "intake_analyst_id", length = 50)
    private String intakeAnalystId;

    @Column(name = "investigation_manager_id", length = 50)
    private String investigationManagerId;

    @Column(name = "investigator_id", length = 50)
    private String investigatorId;

    // Entity relationships for case entities and narratives
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "case_id")
    private List<CaseEntity> entities = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "case_id")
    private List<CaseNarrative> narratives = new ArrayList<>();

    // Constructors
    public Case() {}
    
    public Case(String caseNumber, String title, String description, CaseType caseType) {
        this.caseNumber = caseNumber;
        this.title = title;
        this.description = description;
        this.caseType = caseType;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Lifecycle Callbacks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getCaseNumber() { return caseNumber; }
    public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public CaseType getCaseType() { return caseType; }
    public void setCaseType(CaseType caseType) { this.caseType = caseType; }
    
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    
    public CaseStatus getStatus() { return status; }
    public void setStatus(CaseStatus status) { this.status = status; }
    
    public String getAssignedToUserId() { return assignedToUserId; }
    public void setAssignedToUserId(String assignedToUserId) { this.assignedToUserId = assignedToUserId; }
    
    public String getComplainantName() { return complainantName; }
    public void setComplainantName(String complainantName) { this.complainantName = complainantName; }
    
    public String getComplainantEmail() { return complainantEmail; }
    public void setComplainantEmail(String complainantEmail) { this.complainantEmail = complainantEmail; }
    
    public Long getWorkflowInstanceKey() { return workflowInstanceKey; }
    public void setWorkflowInstanceKey(Long workflowInstanceKey) { this.workflowInstanceKey = workflowInstanceKey; }
    
    public String getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }
    
    // public String getInitialTaskId() { return initialTaskId; }
    // public void setInitialTaskId(String initialTaskId) { this.initialTaskId = initialTaskId; }
    
    // public String getCurrentTaskId() { return currentTaskId; }
    // public void setCurrentTaskId(String currentTaskId) { this.currentTaskId = currentTaskId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(String createdByUserId) { this.createdByUserId = createdByUserId; }
    
    // Entity relationship getters and setters
    public List<WorkItem> getWorkItems() { return workItems; }
    public void setWorkItems(List<WorkItem> workItems) { this.workItems = workItems; }
    
    public List<Allegation> getAllegations() { return allegations; }
    public void setAllegations(List<Allegation> allegations) { this.allegations = allegations; }

    // Helper methods for managing entity relationships
    public void addTransition(CaseTransition transition) {
        transitions.add(transition);
        transition.setCaseEntity(this);
    }
    
    public void addAllegation(Allegation allegation) {
        allegations.add(allegation);
        allegation.setCaseId(this.id);
    }
    
    // Getters and Setters for new fields
    public LocalDate getOccurrenceDate() { return occurrenceDate; }
    public void setOccurrenceDate(LocalDate occurrenceDate) { this.occurrenceDate = occurrenceDate; }
    
    public LocalDate getDateReportedToCiti() { return dateReportedToCiti; }
    public void setDateReportedToCiti(LocalDate dateReportedToCiti) { this.dateReportedToCiti = dateReportedToCiti; }
    
    public LocalDate getDateReceivedByEscalationChannel() { return dateReceivedByEscalationChannel; }
    public void setDateReceivedByEscalationChannel(LocalDate dateReceivedByEscalationChannel) { this.dateReceivedByEscalationChannel = dateReceivedByEscalationChannel; }
    
    public String getComplaintEscalatedBy() { return complaintEscalatedBy; }
    public void setComplaintEscalatedBy(String complaintEscalatedBy) { this.complaintEscalatedBy = complaintEscalatedBy; }
    
    public String getDataSourceId() { return dataSourceId; }
    public void setDataSourceId(String dataSourceId) { this.dataSourceId = dataSourceId; }
    
    public String getClusterCountry() { return clusterCountry; }
    public void setClusterCountry(String clusterCountry) { this.clusterCountry = clusterCountry; }
    
    public Boolean getLegalHold() { return legalHold; }
    public void setLegalHold(Boolean legalHold) { this.legalHold = legalHold; }
    
    public Boolean getOutsideCounsel() { return outsideCounsel; }
    public void setOutsideCounsel(Boolean outsideCounsel) { this.outsideCounsel = outsideCounsel; }
    
    public String getIntakeAnalystId() { return intakeAnalystId; }
    public void setIntakeAnalystId(String intakeAnalystId) { this.intakeAnalystId = intakeAnalystId; }
    
    public String getInvestigationManagerId() { return investigationManagerId; }
    public void setInvestigationManagerId(String investigationManagerId) { this.investigationManagerId = investigationManagerId; }
    
    public String getInvestigatorId() { return investigatorId; }
    public void setInvestigatorId(String investigatorId) { this.investigatorId = investigatorId; }
    
    // Entity and narrative getters/setters and helper methods
    public List<CaseEntity> getEntities() { return entities; }
    public void setEntities(List<CaseEntity> entities) { this.entities = entities; }
    
    public List<CaseNarrative> getNarratives() { return narratives; }
    public void setNarratives(List<CaseNarrative> narratives) { this.narratives = narratives; }
    
    // Helper methods for managing entities and narratives
    public void addEntity(CaseEntity entity) {
        entities.add(entity);
        entity.setCaseId(this.id);
    }
    
    public void addNarrative(CaseNarrative narrative) {
        narratives.add(narrative);
        narrative.setCaseId(this.id);
    }
}