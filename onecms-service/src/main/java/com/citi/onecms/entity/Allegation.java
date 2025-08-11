package com.citi.onecms.entity;

import jakarta.persistence.*;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.PreUpdate;

@Entity
@Table(name = "allegations", schema = "onecms")
public class Allegation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "case_id")
    private Long caseId;
    
    @Column(name = "allegation_type")
    private String allegationType;
    
    @Column(name = "severity")
    @Enumerated(EnumType.STRING)
    private Severity severity;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "department_classification")
    private String departmentClassification;
    
    @Column(name = "assigned_group")
    private String assignedGroup;
    
    @Column(name = "flowable_plan_item_id")
    private String flowablePlanItemId;
    
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Additional fields from frontend requirements
    @Column(name = "allegation_number", length = 100)
    private String allegationNumber;
    
    @Column(name = "investigation_function", length = 50)
    private String investigationFunction;
    
    @Column(name = "subject_entity_id", length = 50)
    private String subjectEntityId;
    
    @Column(name = "allegation_category", length = 20)
    @Enumerated(EnumType.STRING)
    private AllegationCategory allegationCategory;
    
    @Column(name = "grc_taxonomy_1", length = 100)
    private String grcTaxonomy1;
    
    @Column(name = "grc_taxonomy_2", length = 100)
    private String grcTaxonomy2;
    
    @Column(name = "grc_taxonomy_3", length = 100)
    private String grcTaxonomy3;
    
    @Column(name = "grc_taxonomy_4", length = 100)
    private String grcTaxonomy4;
    
    // Constructors
    public Allegation() {}
    
    public Allegation(Long caseId, String allegationType, Severity severity, String description) {
        this.caseId = caseId;
        this.allegationType = allegationType;
        this.severity = severity;
        this.description = description;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Lifecycle callbacks
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getCaseId() {
        return caseId;
    }
    
    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }
    
    public String getAllegationType() {
        return allegationType;
    }
    
    public void setAllegationType(String allegationType) {
        this.allegationType = allegationType;
    }
    
    public Severity getSeverity() {
        return severity;
    }
    
    public void setSeverity(Severity severity) {
        this.severity = severity;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getDepartmentClassification() {
        return departmentClassification;
    }
    
    public void setDepartmentClassification(String departmentClassification) {
        this.departmentClassification = departmentClassification;
    }
    
    public String getAssignedGroup() {
        return assignedGroup;
    }
    
    public void setAssignedGroup(String assignedGroup) {
        this.assignedGroup = assignedGroup;
    }
    
    public String getFlowablePlanItemId() {
        return flowablePlanItemId;
    }
    
    public void setFlowablePlanItemId(String flowablePlanItemId) {
        this.flowablePlanItemId = flowablePlanItemId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Getters and Setters for new fields
    public String getAllegationNumber() { return allegationNumber; }
    public void setAllegationNumber(String allegationNumber) { this.allegationNumber = allegationNumber; }
    
    public String getInvestigationFunction() { return investigationFunction; }
    public void setInvestigationFunction(String investigationFunction) { this.investigationFunction = investigationFunction; }
    
    public String getSubjectEntityId() { return subjectEntityId; }
    public void setSubjectEntityId(String subjectEntityId) { this.subjectEntityId = subjectEntityId; }
    
    public AllegationCategory getAllegationCategory() { return allegationCategory; }
    public void setAllegationCategory(AllegationCategory allegationCategory) { this.allegationCategory = allegationCategory; }
    
    public String getGrcTaxonomy1() { return grcTaxonomy1; }
    public void setGrcTaxonomy1(String grcTaxonomy1) { this.grcTaxonomy1 = grcTaxonomy1; }
    
    public String getGrcTaxonomy2() { return grcTaxonomy2; }
    public void setGrcTaxonomy2(String grcTaxonomy2) { this.grcTaxonomy2 = grcTaxonomy2; }
    
    public String getGrcTaxonomy3() { return grcTaxonomy3; }
    public void setGrcTaxonomy3(String grcTaxonomy3) { this.grcTaxonomy3 = grcTaxonomy3; }
    
    public String getGrcTaxonomy4() { return grcTaxonomy4; }
    public void setGrcTaxonomy4(String grcTaxonomy4) { this.grcTaxonomy4 = grcTaxonomy4; }
    
    // Enum for Allegation Category
    public enum AllegationCategory {
        TRACKED, NON_TRACKED
    }
}