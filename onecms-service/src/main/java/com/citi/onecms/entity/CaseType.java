package com.citi.onecms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "case_types",  schema = "onecms")
public class CaseType {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "case_type_id", unique = true, nullable = false)
    private Long caseTypeId;
    
    @NotBlank
    @Size(max = 50)
    @Column(name = "type_code", unique = true)
    private String typeCode;
    
    @NotBlank
    @Size(max = 100)
    @Column(name = "type_name")
    private String typeName;
    
    @Column(name = "type_description", columnDefinition = "TEXT")
    private String typeDescription;
    
    @Column(name = "default_priority")
    private String defaultPriority;
    
    @Column(name = "sla_hours")
    private Integer slaHours;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public CaseType() {}
    
    public CaseType(String typeCode, String typeName, String typeDescription, 
                   String defaultPriority, Integer slaHours) {
        this.typeCode = typeCode;
        this.typeName = typeName;
        this.typeDescription = typeDescription;
        this.defaultPriority = defaultPriority;
        this.slaHours = slaHours;
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
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Long getCaseTypeId() { return caseTypeId; }
    public void setCaseTypeId(Long caseTypeId) { this.caseTypeId = caseTypeId; }
    
    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
    
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    
    public String getTypeDescription() { return typeDescription; }
    public void setTypeDescription(String typeDescription) { this.typeDescription = typeDescription; }
    
    public String getDefaultPriority() { return defaultPriority; }
    public void setDefaultPriority(String defaultPriority) { this.defaultPriority = defaultPriority; }
    
    public Integer getSlaHours() { return slaHours; }
    public void setSlaHours(Integer slaHours) { this.slaHours = slaHours; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}