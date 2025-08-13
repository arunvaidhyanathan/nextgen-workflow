package com.citi.onecms.dto;

import com.citi.onecms.entity.Priority;
import com.citi.onecms.entity.Severity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Request to create a comprehensive case with allegations, entities, and narratives")
public class CreateCaseWithAllegationsRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Case Basic Information
    @NotBlank(message = "Case title is required")
    @Size(max = 255, message = "Case title must not exceed 255 characters")
    @Schema(description = "Title of the case", example = "Employee Misconduct Investigation")
    private String title;
    
    @Size(max = 1000, message = "Case description must not exceed 1000 characters")
    @Schema(description = "Detailed description of the case", example = "Investigation of multiple allegations against employee John Doe")
    private String description;
    
    @Schema(description = "Priority level of the case", example = "HIGH")
    private Priority priority = Priority.MEDIUM;
    
    @Schema(description = "Case category", example = "COMPLIANCE")
    private String category;
    
    // Case Dates
    @Past(message = "Occurrence date must be in the past")
    @Schema(description = "Date when the incident occurred")
    private LocalDate occurrenceDate;
    
    @Past(message = "Date reported to Citi must be in the past")
    @Schema(description = "Date when incident was reported to Citi")
    private LocalDate dateReportedToCiti;
    
    @Past(message = "Date received by escalation channel must be in the past")
    @Schema(description = "Date received by escalation channel - defaults to today but allows past dates", example = "2025-01-15")
    private LocalDate dateReceivedByEscalationChannel;
    
    // Geographic and Legal Information
    @Schema(description = "Cluster/Country location", example = "APAC/INDIA")
    private String clusterCountry;
    
    @Schema(description = "Legal hold status", example = "true")
    private Boolean legalHold = false;
    
    @Schema(description = "Data source identifier")
    private String dataSourceId;
    
    @Schema(description = "How the complaint was escalated", example = "Employee Reported")
    private String howComplaintEscalated;
    
    // Reporter and Assignment Information
    @NotBlank(message = "Reporter ID is required")
    @Schema(description = "ID of the person reporting the case", example = "alice.intake")
    private String reporterId;
    
    @Schema(description = "User ID to assign the case to", example = "hr.manager")
    private String assignedToUserId;
    
    @Schema(description = "Department to assign the case to", example = "HR")
    private String assignedToDepartment;
    
    // Subject Information
    @Schema(description = "Type of subject", example = "EMPLOYEE")
    private String subjectType;
    
    @Schema(description = "Subject identifier", example = "EMP001")
    private String subjectId;
    
    // Complainant Information (backward compatibility)
    @NotBlank(message = "Complainant name is required")
    @Size(max = 200, message = "Complainant name must not exceed 200 characters")
    @Schema(description = "Name of the person filing the complaint", example = "Jane Smith")
    private String complainantName;
    
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Complainant email must not exceed 255 characters")
    @Schema(description = "Email of the person filing the complaint", example = "jane.smith@company.com")
    private String complainantEmail;
    
    // Investigation Unit Details
    @Schema(description = "Intake analyst assigned", example = "Sophie Smith (SS12345)")
    private String intakeAnalyst;
    
    @Schema(description = "Investigation manager", example = "Tyler Keith (TK12345)")
    private String investigationManager;
    
    @Schema(description = "Investigator assigned", example = "John Doe (JD12345)")
    private String investigator;
    
    @Schema(description = "Assignment group", example = "APAC")
    private String assignmentGroup;
    
    @Schema(description = "Outside counsel required", example = "false")
    private Boolean outsideCounsel = false;
    
    // Complex nested objects
    @Valid
    @Schema(description = "List of entities (people/organizations) involved in this case")
    private List<EntityRequest> entities;
    
    @NotEmpty(message = "At least one allegation is required")
    @Size(max = 10, message = "Maximum 10 allegations allowed per case")
    @Valid
    @Schema(description = "List of allegations associated with this case")
    private List<AllegationRequest> allegations;
    
    @Valid
    @Schema(description = "List of narratives associated with this case")
    private List<NarrativeRequest> narratives;
    
    // Nested class for entity requests (Person/Organization)
    @Schema(description = "Entity (Person or Organization) details")
    public static class EntityRequest implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        @NotBlank(message = "Entity type is required")
        @Schema(description = "Type of entity", example = "Person", allowableValues = {"Person", "Organisation"})
        private String type;
        
        @NotBlank(message = "Relationship type is required")
        @Schema(description = "Relationship to case", example = "Complainant, Subject")
        private String relationshipType;
        
        // Person fields
        @Schema(description = "SOEID for person entities")
        private String soeid;
        
        @Schema(description = "GEID for person entities")
        private String geid;
        
        @Schema(description = "First name", example = "John")
        private String firstName;
        
        @Schema(description = "Middle name", example = "Robert")
        private String middleName;
        
        @Schema(description = "Last name", example = "Doe")
        private String lastName;
        
        @Schema(description = "Organization name for organization entities")
        private String organizationName;
        
        // Contact Information
        @Schema(description = "Full address")
        private String address;
        
        @Schema(description = "City")
        private String city;
        
        @Schema(description = "State")
        private String state;
        
        @Schema(description = "Zip code")
        private String zip;
        
        @Email
        @Schema(description = "Email address")
        private String emailAddress;
        
        @Schema(description = "Phone number")
        private String phoneNumber;
        
        @Schema(description = "Preferred contact method")
        private String preferredContactMethod;
        
        // Employment Information
        @Schema(description = "GOC (Global Operations Center)")
        private String goc;
        
        @Schema(description = "Manager name and SOEID")
        private String manager;
        
        @Schema(description = "Hire date")
        private LocalDate hireDate;
        
        @Schema(description = "HR responsible person")
        private String hrResponsible;
        
        @Schema(description = "Legal vehicle")
        private String legalVehicle;
        
        @Schema(description = "Managed segment")
        private String managedSegment;
        
        @Schema(description = "Relationship to Citi")
        private String relationshipToCiti;
        
        @Schema(description = "Anonymous flag")
        private Boolean anonymous = false;
        
        // Investigation Function
        @Schema(description = "Investigation function", example = "ER")
        private String investigationFunction = "ER";
        
        // Constructors
        public EntityRequest() {}
        
        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
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
        
        public String getOrganizationName() { return organizationName; }
        public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
        
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        
        public String getZip() { return zip; }
        public void setZip(String zip) { this.zip = zip; }
        
        public String getEmailAddress() { return emailAddress; }
        public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }
        
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        
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
        
        public String getInvestigationFunction() { return investigationFunction; }
        public void setInvestigationFunction(String investigationFunction) { this.investigationFunction = investigationFunction; }
    }
    
    // Enhanced allegation request class
    @Schema(description = "Allegation details with GRC taxonomy")
    public static class AllegationRequest implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        @NotBlank(message = "Allegation type is required")
        @Schema(description = "Type of allegation", example = "Tracked", allowableValues = {"Tracked", "Non-Tracked"})
        private String allegationType;
        
        @Schema(description = "Subject person reference (SOEID or entity reference)")
        private String subject;
        
        @Schema(description = "GRC Taxonomy Level 1")
        private String grcTaxonomyLevel1;
        
        @Schema(description = "GRC Taxonomy Level 2")
        private String grcTaxonomyLevel2;
        
        @Schema(description = "GRC Taxonomy Level 3")
        private String grcTaxonomyLevel3;
        
        @Schema(description = "GRC Taxonomy Level 4")
        private String grcTaxonomyLevel4;
        
        @Schema(description = "Investigation function", example = "ER")
        private String investigationFunction = "ER";
        
        @Schema(description = "Severity level of the allegation", example = "HIGH")
        private Severity severity = Severity.MEDIUM;
        
        @Size(max = 2000, message = "Allegation description must not exceed 2000 characters")
        @Schema(description = "Detailed description of the allegation")
        private String description;
        
        @Schema(description = "Allegation narrative for detailed explanation")
        private String narrative;
        
        // Constructors
        public AllegationRequest() {}
        
        // Getters and Setters
        public String getAllegationType() { return allegationType; }
        public void setAllegationType(String allegationType) { this.allegationType = allegationType; }
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        
        public String getGrcTaxonomyLevel1() { return grcTaxonomyLevel1; }
        public void setGrcTaxonomyLevel1(String grcTaxonomyLevel1) { this.grcTaxonomyLevel1 = grcTaxonomyLevel1; }
        
        public String getGrcTaxonomyLevel2() { return grcTaxonomyLevel2; }
        public void setGrcTaxonomyLevel2(String grcTaxonomyLevel2) { this.grcTaxonomyLevel2 = grcTaxonomyLevel2; }
        
        public String getGrcTaxonomyLevel3() { return grcTaxonomyLevel3; }
        public void setGrcTaxonomyLevel3(String grcTaxonomyLevel3) { this.grcTaxonomyLevel3 = grcTaxonomyLevel3; }
        
        public String getGrcTaxonomyLevel4() { return grcTaxonomyLevel4; }
        public void setGrcTaxonomyLevel4(String grcTaxonomyLevel4) { this.grcTaxonomyLevel4 = grcTaxonomyLevel4; }
        
        public String getInvestigationFunction() { return investigationFunction; }
        public void setInvestigationFunction(String investigationFunction) { this.investigationFunction = investigationFunction; }
        
        public Severity getSeverity() { return severity; }
        public void setSeverity(Severity severity) { this.severity = severity; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getNarrative() { return narrative; }
        public void setNarrative(String narrative) { this.narrative = narrative; }
    }
    
    // Narrative request class
    @Schema(description = "Narrative details")
    public static class NarrativeRequest implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        @NotBlank(message = "Narrative type is required")
        @Schema(description = "Type of narrative", example = "Original Claim")
        private String type;
        
        @Schema(description = "Title of the narrative")
        private String title;
        
        @NotBlank(message = "Narrative content is required")
        @Schema(description = "Narrative content with unlimited length and multilingual support")
        private String narrative;
        
        @Schema(description = "Investigation function", example = "ER")
        private String investigationFunction = "ER";
        
        @Schema(description = "Date when narrative was added")
        private LocalDateTime addedOn;
        
        // Constructors
        public NarrativeRequest() {}
        
        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getNarrative() { return narrative; }
        public void setNarrative(String narrative) { this.narrative = narrative; }
        
        public String getInvestigationFunction() { return investigationFunction; }
        public void setInvestigationFunction(String investigationFunction) { this.investigationFunction = investigationFunction; }
        
        public LocalDateTime getAddedOn() { return addedOn; }
        public void setAddedOn(LocalDateTime addedOn) { this.addedOn = addedOn; }
    }
    
    // Constructors
    public CreateCaseWithAllegationsRequest() {}
    
    // Getters and Setters for all fields
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public LocalDate getOccurrenceDate() { return occurrenceDate; }
    public void setOccurrenceDate(LocalDate occurrenceDate) { this.occurrenceDate = occurrenceDate; }
    
    public LocalDate getDateReportedToCiti() { return dateReportedToCiti; }
    public void setDateReportedToCiti(LocalDate dateReportedToCiti) { this.dateReportedToCiti = dateReportedToCiti; }
    
    public LocalDate getDateReceivedByEscalationChannel() { return dateReceivedByEscalationChannel; }
    public void setDateReceivedByEscalationChannel(LocalDate dateReceivedByEscalationChannel) { this.dateReceivedByEscalationChannel = dateReceivedByEscalationChannel; }
    
    public String getClusterCountry() { return clusterCountry; }
    public void setClusterCountry(String clusterCountry) { this.clusterCountry = clusterCountry; }
    
    public Boolean getLegalHold() { return legalHold; }
    public void setLegalHold(Boolean legalHold) { this.legalHold = legalHold; }
    
    public String getDataSourceId() { return dataSourceId; }
    public void setDataSourceId(String dataSourceId) { this.dataSourceId = dataSourceId; }
    
    public String getHowComplaintEscalated() { return howComplaintEscalated; }
    public void setHowComplaintEscalated(String howComplaintEscalated) { this.howComplaintEscalated = howComplaintEscalated; }
    
    public String getReporterId() { return reporterId; }
    public void setReporterId(String reporterId) { this.reporterId = reporterId; }
    
    public String getAssignedToUserId() { return assignedToUserId; }
    public void setAssignedToUserId(String assignedToUserId) { this.assignedToUserId = assignedToUserId; }
    
    public String getAssignedToDepartment() { return assignedToDepartment; }
    public void setAssignedToDepartment(String assignedToDepartment) { this.assignedToDepartment = assignedToDepartment; }
    
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    
    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }
    
    public String getComplainantName() { return complainantName; }
    public void setComplainantName(String complainantName) { this.complainantName = complainantName; }
    
    public String getComplainantEmail() { return complainantEmail; }
    public void setComplainantEmail(String complainantEmail) { this.complainantEmail = complainantEmail; }
    
    public String getIntakeAnalyst() { return intakeAnalyst; }
    public void setIntakeAnalyst(String intakeAnalyst) { this.intakeAnalyst = intakeAnalyst; }
    
    public String getInvestigationManager() { return investigationManager; }
    public void setInvestigationManager(String investigationManager) { this.investigationManager = investigationManager; }
    
    public String getInvestigator() { return investigator; }
    public void setInvestigator(String investigator) { this.investigator = investigator; }
    
    public String getAssignmentGroup() { return assignmentGroup; }
    public void setAssignmentGroup(String assignmentGroup) { this.assignmentGroup = assignmentGroup; }
    
    public Boolean getOutsideCounsel() { return outsideCounsel; }
    public void setOutsideCounsel(Boolean outsideCounsel) { this.outsideCounsel = outsideCounsel; }
    
    public List<EntityRequest> getEntities() { return entities; }
    public void setEntities(List<EntityRequest> entities) { this.entities = entities; }
    
    public List<AllegationRequest> getAllegations() { return allegations; }
    public void setAllegations(List<AllegationRequest> allegations) { this.allegations = allegations; }
    
    public List<NarrativeRequest> getNarratives() { return narratives; }
    public void setNarratives(List<NarrativeRequest> narratives) { this.narratives = narratives; }
}