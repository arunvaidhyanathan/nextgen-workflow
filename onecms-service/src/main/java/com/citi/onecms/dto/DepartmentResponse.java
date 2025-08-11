package com.citi.onecms.dto;

import com.citi.onecms.entity.Department;

import java.time.LocalDateTime;

public class DepartmentResponse {
    private Long id;
    private Long departmentId;
    private String departmentCode;
    private String departmentName;
    private String departmentDescription;
    private String managerUserId;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String departmentRegion;
    private String departmentFunction;

    public DepartmentResponse() {}

    public DepartmentResponse(Department department) {
        this.id = department.getId();
        this.departmentId = department.getDepartmentId();
        this.departmentCode = department.getDepartmentCode();
        this.departmentName = department.getDepartmentName();
        this.departmentDescription = department.getDepartmentDescription();
        this.managerUserId = department.getManagerUserId();
        this.isActive = department.getIsActive();
        this.createdAt = department.getCreatedAt();
        this.updatedAt = department.getUpdatedAt();
        this.departmentRegion = department.getDepartmentRegion();
        this.departmentFunction = department.getDepartmentFunction();
    }

    // Note: UserSummaryResponse removed - manager data now managed by entitlement-service

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