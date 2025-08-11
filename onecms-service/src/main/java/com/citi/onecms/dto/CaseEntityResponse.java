package com.citi.onecms.dto;

import com.citi.onecms.entity.CaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseEntityResponse {
    private Long id;
    private String entityId;
    private String caseId;
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
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Computed field for display name
    private String displayName;
}