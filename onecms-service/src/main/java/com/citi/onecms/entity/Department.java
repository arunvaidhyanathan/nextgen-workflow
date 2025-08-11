package com.citi.onecms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "departments",  schema = "onecms")
public class Department {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "department_id", unique = true, nullable = false)
    private Long departmentId;
    
    @NotBlank
    @Size(max = 50)
    @Column(name = "department_code", unique = true)
    private String departmentCode;
    
    @NotBlank
    @Size(max = 100)
    @Column(name = "department_name")
    private String departmentName;
    
    @Column(name = "department_description", columnDefinition = "TEXT")
    private String departmentDescription;
    
    @Column(name = "manager_user_id", length = 100)
    private String managerUserId;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Additional fields from frontend requirements
    @Column(name = "department_region", length = 50)
    private String departmentRegion;
    
    @Column(name = "department_function", length = 50)
    private String departmentFunction;
    
    // Constructors
    public Department() {}
    
    public Department(String departmentCode, String departmentName, String departmentDescription) {
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.departmentDescription = departmentDescription;
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
    
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    
    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
    
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    
    public String getDepartmentDescription() { return departmentDescription; }
    public void setDepartmentDescription(String departmentDescription) { this.departmentDescription = departmentDescription; }
    
    public String getManagerUserId() { return managerUserId; }
    public void setManagerUserId(String managerUserId) { this.managerUserId = managerUserId; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getDepartmentRegion() { return departmentRegion; }
    public void setDepartmentRegion(String departmentRegion) { this.departmentRegion = departmentRegion; }
    
    public String getDepartmentFunction() { return departmentFunction; }
    public void setDepartmentFunction(String departmentFunction) { this.departmentFunction = departmentFunction; }
}