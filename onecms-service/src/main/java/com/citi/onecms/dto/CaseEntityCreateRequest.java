package com.citi.onecms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class CaseEntityCreateRequest {
    
    @NotBlank(message = "Case ID is required")
    private String caseId;
    
    @NotBlank(message = "Entity type is required")
    private String entityType; // PERSON or ORGANIZATION
    
    private String investigationFunction;
    
    @NotBlank(message = "Relationship type is required")
    private String relationshipType;
    
    // Person fields
    private String soeid;
    private String geid;
    
    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;
    
    @Size(max = 100)
    private String middleName;
    
    @Size(max = 100)
    private String lastName;
    
    @Email(message = "Invalid email address")
    @Size(max = 255)
    private String emailAddress;
    
    @Size(max = 50)
    private String phoneNumber;
    
    private String address;
    
    @Size(max = 100)
    private String city;
    
    @Size(max = 100)
    private String state;
    
    @Size(max = 20)
    private String zipCode;
    
    // Organization fields
    @Size(max = 255)
    private String organizationName;
    
    @Size(max = 100)
    private String organizationType;
    
    // Enhanced fields from UI design
    @Size(max = 256, message = "Preferred contact method must be 256 characters or less")
    private String preferredContactMethod;
    
    @Size(max = 100)
    private String goc;
    
    @Size(max = 255)
    private String manager;
    
    private LocalDate hireDate;
    
    @Size(max = 255)
    private String hrResponsible;
    
    private String legalVehicle;
    private String managedSegment;
    private String relationshipToCiti;
    private Boolean anonymous = false;
    
    // Getters and Setters
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
}