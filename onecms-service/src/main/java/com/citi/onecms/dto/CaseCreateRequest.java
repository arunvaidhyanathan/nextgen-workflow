package com.citi.onecms.dto;   

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class CaseCreateRequest {
    @NotBlank
    @Size(max = 255)
    private String title;
    
    private String description;
    
    @NotBlank
    private String caseTypeCode;
    
    @NotBlank
    private String allegationType;
    
    @NotBlank
    private String severity;
    
    @NotBlank
    private String priority;
    
    private String complainantName;
    private String complainantEmail;
    
    // Additional fields from frontend requirements
    private LocalDate occurrenceDate;
    private LocalDate dateReportedToCiti;
    private LocalDate dateReceivedByEscalationChannel;
    private String complaintEscalatedBy;
    private String dataSourceId;
    private String clusterCountry;
    private Boolean legalHold = false;
    private Boolean outsideCounsel = false;
    private Long intakeAnalystId;
    private Long investigationManagerId;
    private Long investigatorId;
    
    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCaseTypeCode() { return caseTypeCode; }
    public void setCaseTypeCode(String caseTypeCode) { this.caseTypeCode = caseTypeCode; }
    
    public String getAllegationType() { return allegationType; }
    public void setAllegationType(String allegationType) { this.allegationType = allegationType; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    
    public String getComplainantName() { return complainantName; }
    public void setComplainantName(String complainantName) { this.complainantName = complainantName; }
    
    public String getComplainantEmail() { return complainantEmail; }
    public void setComplainantEmail(String complainantEmail) { this.complainantEmail = complainantEmail; }
    
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
    
    public Long getIntakeAnalystId() { return intakeAnalystId; }
    public void setIntakeAnalystId(Long intakeAnalystId) { this.intakeAnalystId = intakeAnalystId; }
    
    public Long getInvestigationManagerId() { return investigationManagerId; }
    public void setInvestigationManagerId(Long investigationManagerId) { this.investigationManagerId = investigationManagerId; }
    
    public Long getInvestigatorId() { return investigatorId; }
    public void setInvestigatorId(Long investigatorId) { this.investigatorId = investigatorId; }
}