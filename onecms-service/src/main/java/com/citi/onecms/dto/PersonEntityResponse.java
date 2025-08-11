package com.citi.onecms.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PersonEntityResponse {
    
    private Long id;
    private String entityId;
    private String caseId;
    private String entityType;
    private String investigationFunction;
    private String relationshipType;
    
    // Person fields
    private String soeid;
    private String geid;
    private String firstName;
    private String middleName;
    private String lastName;
    private String emailAddress;
    private String phoneNumber;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    
    // Organization fields
    private String organizationName;
    private String organizationType;
    
    // Enhanced fields
    private String preferredContactMethod;
    private String goc;
    private String manager;
    private LocalDate hireDate;
    private String hrResponsible;
    private String legalVehicle;
    private String managedSegment;
    private String relationshipToCiti;
    private Boolean anonymous;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String displayName;
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    
    public String getInvestigationFunction() { return investigationFunction; }
    public void setInvestigationFunction(String investigationFunction) { this.investigationFunction = investigationFunction; }
    
    public String getRelationshipType() { return relationshipType; }
    public void setRelationshipType(String relationshipType) { this.relationshipType = relationshipType; }
    
    public String getSoeid() { return soeid; }
    public void setSoeid(String soeid) { this.soeid = soeid; }
    
    public String getGeid() { return geid; }
    public void setGeid(String geid) { this.geid = geid; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    
    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
    
    public String getOrganizationType() { return organizationType; }
    public void setOrganizationType(String organizationType) { this.organizationType = organizationType; }
    
    public String getPreferredContactMethod() { return preferredContactMethod; }
    public void setPreferredContactMethod(String preferredContactMethod) { this.preferredContactMethod = preferredContactMethod; }
    
    public String getGoc() { return goc; }
    public void setGoc(String goc) { this.goc = goc; }
    
    public String getManager() { return manager; }
    public void setManager(String manager) { this.manager = manager; }
    
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    
    public String getHrResponsible() { return hrResponsible; }
    public void setHrResponsible(String hrResponsible) { this.hrResponsible = hrResponsible; }
    
    public String getLegalVehicle() { return legalVehicle; }
    public void setLegalVehicle(String legalVehicle) { this.legalVehicle = legalVehicle; }
    
    public String getManagedSegment() { return managedSegment; }
    public void setManagedSegment(String managedSegment) { this.managedSegment = managedSegment; }
    
    public String getRelationshipToCiti() { return relationshipToCiti; }
    public void setRelationshipToCiti(String relationshipToCiti) { this.relationshipToCiti = relationshipToCiti; }
    
    public Boolean getAnonymous() { return anonymous; }
    public void setAnonymous(Boolean anonymous) { this.anonymous = anonymous; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}