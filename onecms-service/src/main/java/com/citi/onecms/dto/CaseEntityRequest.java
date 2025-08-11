package com.citi.onecms.dto;

import com.citi.onecms.entity.CaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseEntityRequest {
    
    @NotBlank(message = "Case ID is required")
    private String caseId;
    
    @NotNull(message = "Entity type is required")
    private CaseEntity.EntityType entityType;
    
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
}