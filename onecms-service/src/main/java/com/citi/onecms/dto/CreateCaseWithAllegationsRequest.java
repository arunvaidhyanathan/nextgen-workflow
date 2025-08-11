package com.citi.onecms.dto;

import com.citi.onecms.entity.Priority;
import com.citi.onecms.entity.Severity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.List;

@Schema(description = "Request to create a case with multiple allegations")
public class CreateCaseWithAllegationsRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @NotBlank(message = "Case title is required")
    @Size(max = 255, message = "Case title must not exceed 255 characters")
    @Schema(description = "Title of the case", example = "Employee Misconduct Investigation")
    private String title;
    
    @Size(max = 1000, message = "Case description must not exceed 1000 characters")
    @Schema(description = "Detailed description of the case", example = "Investigation of multiple allegations against employee John Doe")
    private String description;
    
    @Schema(description = "Priority level of the case", example = "HIGH")
    private Priority priority = Priority.MEDIUM;
    
    @NotBlank(message = "Complainant name is required")
    @Size(max = 200, message = "Complainant name must not exceed 200 characters")
    @Schema(description = "Name of the person filing the complaint", example = "Jane Smith")
    private String complainantName;
    
    @Size(max = 255, message = "Complainant email must not exceed 255 characters")
    @Schema(description = "Email of the person filing the complaint", example = "jane.smith@company.com")
    private String complainantEmail;
    
    @NotEmpty(message = "At least one allegation is required")
    @Size(max = 10, message = "Maximum 10 allegations allowed per case")
    @Valid
    @Schema(description = "List of allegations associated with this case")
    private List<AllegationRequest> allegations;
    
    // Nested class for allegation requests
    @Schema(description = "Allegation details")
    public static class AllegationRequest implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        @NotBlank(message = "Allegation type is required")
        @Size(max = 100, message = "Allegation type must not exceed 100 characters")
        @Schema(description = "Type of allegation", example = "Sexual Harassment")
        private String allegationType;
        
        @Schema(description = "Severity level of the allegation", example = "HIGH")
        private Severity severity = Severity.MEDIUM;
        
        @Size(max = 1000, message = "Allegation description must not exceed 1000 characters")
        @Schema(description = "Detailed description of the allegation", example = "Inappropriate behavior towards female colleagues")
        private String description;
        
        // Constructors
        public AllegationRequest() {}
        
        // Getters and Setters
        public String getAllegationType() { return allegationType; }
        public void setAllegationType(String allegationType) { this.allegationType = allegationType; }
        
        public Severity getSeverity() { return severity; }
        public void setSeverity(Severity severity) { this.severity = severity; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    // Constructors
    public CreateCaseWithAllegationsRequest() {}
    
    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    
    public String getComplainantName() { return complainantName; }
    public void setComplainantName(String complainantName) { this.complainantName = complainantName; }
    
    public String getComplainantEmail() { return complainantEmail; }
    public void setComplainantEmail(String complainantEmail) { this.complainantEmail = complainantEmail; }
    
    public List<AllegationRequest> getAllegations() { return allegations; }
    public void setAllegations(List<AllegationRequest> allegations) { this.allegations = allegations; }
}