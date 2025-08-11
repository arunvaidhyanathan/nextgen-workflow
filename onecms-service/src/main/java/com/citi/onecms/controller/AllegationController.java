package com.citi.onecms.controller;

import com.citi.onecms.dto.CaseWithAllegationsResponse;
import com.citi.onecms.entity.Allegation;
import com.citi.onecms.entity.Severity;
import com.citi.onecms.repository.AllegationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/allegations")
@Tag(name = "Allegation Management", description = "APIs for managing individual allegations")
public class AllegationController {
    
    @Autowired
    private AllegationRepository allegationRepository;
    
    @GetMapping("/{allegationId}")
    @Operation(summary = "Get allegation by ID", 
               description = "Retrieves a specific allegation by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Allegation found"),
        @ApiResponse(responseCode = "404", description = "Allegation not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('ALLEGATION_VIEWER') or hasRole('ADMIN')")
    public ResponseEntity<CaseWithAllegationsResponse.AllegationResponse> getAllegation(
            @Parameter(description = "Allegation ID", example = "ALG-2024-001")
            @PathVariable String allegationId) {
        
        Allegation allegation = allegationRepository.findById(Long.parseLong(allegationId))
            .orElseThrow(() -> new RuntimeException("Allegation not found: " + allegationId));
        
        CaseWithAllegationsResponse.AllegationResponse response = convertToAllegationResponse(allegation);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/case/{caseId}")
    @Operation(summary = "Get allegations by case ID", 
               description = "Retrieves all allegations associated with a specific case")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Allegations retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('ALLEGATION_VIEWER') or hasRole('ADMIN')")
    public ResponseEntity<List<CaseWithAllegationsResponse.AllegationResponse>> getAllegationsByCase(
            @Parameter(description = "Case ID", example = "CMS-2024-001")
            @PathVariable String caseId) {
        
        List<Allegation> allegations = allegationRepository.findByCaseId(Long.parseLong(caseId));
        List<CaseWithAllegationsResponse.AllegationResponse> responses = allegations.stream()
            .map(this::convertToAllegationResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/type/{allegationType}")
    @Operation(summary = "Get allegations by type", 
               description = "Retrieves all allegations of a specific type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Allegations retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('ALLEGATION_VIEWER') or hasRole('ADMIN')")
    public ResponseEntity<List<CaseWithAllegationsResponse.AllegationResponse>> getAllegationsByType(
            @Parameter(description = "Allegation type", example = "Sexual Harassment")
            @PathVariable String allegationType) {
        
        List<Allegation> allegations = allegationRepository.findByAllegationType(allegationType);
        List<CaseWithAllegationsResponse.AllegationResponse> responses = allegations.stream()
            .map(this::convertToAllegationResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/severity/{severity}")
    @Operation(summary = "Get allegations by severity", 
               description = "Retrieves all allegations with a specific severity level")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Allegations retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid severity level"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('ALLEGATION_VIEWER') or hasRole('ADMIN')")
    public ResponseEntity<List<CaseWithAllegationsResponse.AllegationResponse>> getAllegationsBySeverity(
            @Parameter(description = "Severity level", example = "HIGH")
            @PathVariable String severity) {
        
        try {
            Severity severityEnum = Severity.valueOf(severity.toUpperCase());
            List<Allegation> allegations = allegationRepository.findBySeverity(severityEnum);
            List<CaseWithAllegationsResponse.AllegationResponse> responses = allegations.stream()
                .map(this::convertToAllegationResponse)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid severity level: " + severity);
        }
    }
    
    @GetMapping("/department/{department}")
    @Operation(summary = "Get allegations by department classification", 
               description = "Retrieves all allegations classified for a specific department")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Allegations retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('ALLEGATION_VIEWER') or hasRole('ADMIN')")
    public ResponseEntity<List<CaseWithAllegationsResponse.AllegationResponse>> getAllegationsByDepartment(
            @Parameter(description = "Department classification", example = "HR")
            @PathVariable String department) {
        
        List<Allegation> allegations = allegationRepository.findByDepartmentClassification(department);
        List<CaseWithAllegationsResponse.AllegationResponse> responses = allegations.stream()
            .map(this::convertToAllegationResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping
    @Operation(summary = "Get all allegations", 
               description = "Retrieves all allegations in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Allegations retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('ALLEGATION_VIEWER') or hasRole('ADMIN')")
    public ResponseEntity<List<CaseWithAllegationsResponse.AllegationResponse>> getAllAllegations() {
        List<Allegation> allegations = allegationRepository.findAll();
        List<CaseWithAllegationsResponse.AllegationResponse> responses = allegations.stream()
            .map(this::convertToAllegationResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{allegationId}/classification")
    @Operation(summary = "Update allegation classification", 
               description = "Updates the department classification and assigned group for an allegation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Classification updated successfully"),
        @ApiResponse(responseCode = "404", description = "Allegation not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('ALLEGATION_CLASSIFIER') or hasRole('ADMIN')")
    public ResponseEntity<CaseWithAllegationsResponse.AllegationResponse> updateAllegationClassification(
            @Parameter(description = "Allegation ID", example = "ALG-2024-001")
            @PathVariable String allegationId,
            @Parameter(description = "Department classification", example = "HR")
            @RequestParam String departmentClassification,
            @Parameter(description = "Assigned group", example = "HR_SPECIALIST")
            @RequestParam String assignedGroup) {
        
        Allegation allegation = allegationRepository.findById(Long.parseLong(allegationId))
            .orElseThrow(() -> new RuntimeException("Allegation not found: " + allegationId));
        
        allegation.setDepartmentClassification(departmentClassification);
        allegation.setAssignedGroup(assignedGroup);
        
        Allegation updatedAllegation = allegationRepository.save(allegation);
        CaseWithAllegationsResponse.AllegationResponse response = convertToAllegationResponse(updatedAllegation);
        
        return ResponseEntity.ok(response);
    }
    
    private CaseWithAllegationsResponse.AllegationResponse convertToAllegationResponse(Allegation allegation) {
        CaseWithAllegationsResponse.AllegationResponse response = new CaseWithAllegationsResponse.AllegationResponse();
        response.setAllegationId(allegation.getId() != null ? allegation.getId().toString() : null);
        response.setAllegationType(allegation.getAllegationType());
        response.setSeverity(allegation.getSeverity());
        response.setDescription(allegation.getDescription());
        response.setDepartmentClassification(allegation.getDepartmentClassification());
        response.setAssignedGroup(allegation.getAssignedGroup());
        response.setFlowablePlanItemId(allegation.getFlowablePlanItemId());
        response.setCreatedAt(allegation.getCreatedAt());
        response.setUpdatedAt(allegation.getUpdatedAt());
        return response;
    }
}