package com.citi.onecms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "case_summaries", schema = "onecms",
       uniqueConstraints = @UniqueConstraint(name = "uq_case_summaries_case_status", 
                                           columnNames = {"case_id", "status_id"}))
public class CaseSummary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "case_summaries_seq")
    @SequenceGenerator(name = "case_summaries_seq", sequenceName = "case_summaries_seq", allocationSize = 1, schema = "onecms")
    @Column(name = "case_summaries_id")
    private Long caseSummariesId;
    
    @NotNull
    @Column(name = "case_id", nullable = false)
    private Long caseId;
    
    @NotBlank
    @Size(max = 255)
    @Column(name = "status_id", nullable = false)
    private String statusId;
    
    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // Relationship to the Case entity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Case caseEntity;
    
    // Constructors
    public CaseSummary() {}
    
    public CaseSummary(Long caseId, String statusId, String summaryText) {
        this.caseId = caseId;
        this.statusId = statusId;
        this.summaryText = summaryText;
        this.createdAt = LocalDateTime.now();
    }
    
    // Lifecycle Callbacks
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    // Getters and Setters
    public Long getCaseSummariesId() {
        return caseSummariesId;
    }
    
    public void setCaseSummariesId(Long caseSummariesId) {
        this.caseSummariesId = caseSummariesId;
    }
    
    public Long getCaseId() {
        return caseId;
    }
    
    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }
    
    public String getStatusId() {
        return statusId;
    }
    
    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }
    
    public String getSummaryText() {
        return summaryText;
    }
    
    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Case getCaseEntity() {
        return caseEntity;
    }
    
    public void setCaseEntity(Case caseEntity) {
        this.caseEntity = caseEntity;
    }
    
    // Status ID constants for different workflow steps
    public static final String STATUS_LLM_INITIAL = "LLM_INITIAL";
    public static final String STATUS_EO_INTAKE_ANALYST = "EO_INTAKE_ANALYST";
    public static final String STATUS_EO_INTAKE_DIRECTOR = "EO_INTAKE_DIRECTOR";
    public static final String STATUS_INVESTIGATION_COMPLETE = "INVESTIGATION_COMPLETE";
}