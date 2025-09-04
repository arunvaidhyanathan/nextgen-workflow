package com.citi.onecms.dto;

import com.citi.onecms.dto.CreateCaseWithAllegationsRequest.AllegationRequest;
import com.citi.onecms.dto.CreateCaseWithAllegationsRequest.EntityRequest;
import com.citi.onecms.dto.CreateCaseWithAllegationsRequest.NarrativeRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;
import java.util.List;

@Schema(description = "Request to enhance an existing case with allegations, entities, and narratives for Option 1 two-phase approach")
public class EnhancedCreateCaseRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @NotBlank(message = "Case ID is required")
    @JsonProperty("case_id")
    @Schema(description = "Case ID (case number) to enhance", 
            example = "CMS-2025-000009", 
            required = true)
    private String caseId;
    
    @Valid
    @Schema(description = "List of allegations to add to the case")
    private List<AllegationRequest> allegations;
    
    @Valid
    @Schema(description = "List of entities (people/organizations) to add to the case")
    private List<EntityRequest> entities;
    
    @Valid
    @Schema(description = "List of narratives to add to the case")
    private List<NarrativeRequest> narratives;
    
    // Constructors
    public EnhancedCreateCaseRequest() {}
    
    public EnhancedCreateCaseRequest(String caseId, 
                                   List<AllegationRequest> allegations,
                                   List<EntityRequest> entities,
                                   List<NarrativeRequest> narratives) {
        this.caseId = caseId;
        this.allegations = allegations;
        this.entities = entities;
        this.narratives = narratives;
    }
    
    // Getters and Setters
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    
    public List<AllegationRequest> getAllegations() { return allegations; }
    public void setAllegations(List<AllegationRequest> allegations) { this.allegations = allegations; }
    
    public List<EntityRequest> getEntities() { return entities; }
    public void setEntities(List<EntityRequest> entities) { this.entities = entities; }
    
    public List<NarrativeRequest> getNarratives() { return narratives; }
    public void setNarratives(List<NarrativeRequest> narratives) { this.narratives = narratives; }
}