package com.citi.onecms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseNarrativeRequest {
    
    @NotBlank(message = "Case ID is required")
    private String caseId;
    
    private String investigationFunction;
    
    private String narrativeType;
    
    private String narrativeTitle;
    
    @NotBlank(message = "Narrative text is required")
    private String narrativeText;
    
    private Boolean isRecalled = false;
}