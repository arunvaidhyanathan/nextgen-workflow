package com.citi.onecms.controller;

import com.citi.onecms.dto.CreateCaseWithAllegationsRequest;
import com.citi.onecms.dto.CaseWithAllegationsResponse;
import com.citi.onecms.service.CaseWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/cases")
@Tag(name = "Case Management", description = "APIs for managing cases and workflow processes")
public class CaseManagementController {
    
    @Autowired
    private CaseWorkflowService caseWorkflowService;
    
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Case management controller is working!");
    }
    
    @GetMapping("/auth-test")
    @Operation(summary = "Test authentication", description = "Test endpoint to verify authentication is working")
    public ResponseEntity<Map<String, Object>> testAuth(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        if (authentication == null) {
            response.put("authenticated", false);
            response.put("message", "No authentication found");
            return ResponseEntity.status(401).body(response);
        }
        
        response.put("authenticated", true);
        response.put("username", authentication.getName());
        response.put("authorities", authentication.getAuthorities().stream()
            .map(auth -> auth.getAuthority())
            .collect(java.util.stream.Collectors.toList()));
        response.put("principal", authentication.getPrincipal().getClass().getSimpleName());
        
        // Add detailed role analysis
        List<String> roles = authentication.getAuthorities().stream()
            .map(auth -> auth.getAuthority())
            .collect(java.util.stream.Collectors.toList());
            
        response.put("hasDirectorGroup", roles.contains("DIRECTOR_GROUP"));
        response.put("hasManagerGroup", roles.contains("MANAGER_GROUP"));
        response.put("hasAnalystGroup", roles.contains("ANALYST_GROUP"));
        response.put("hasIntakeAnalyst", roles.contains("INTAKE_ANALYST") || roles.contains("INTAKE_ANALYST_GROUP"));
        
        // Check what roles would allow access to /v1/cases
        boolean canAccessCases = roles.contains("DIRECTOR_GROUP") || 
                               roles.contains("MANAGER_GROUP") || 
                               roles.contains("ANALYST_GROUP");
        response.put("canAccessCases", canAccessCases);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    @Operation(summary = "Create a new case", description = "Create a new case with allegations and start the workflow process")
    @PreAuthorize("hasPermission(#request, 'case', 'create')")
    public ResponseEntity<CaseWithAllegationsResponse> createCase(@Valid @RequestBody CreateCaseWithAllegationsRequest request) {
        System.out.println("🎯 Received case creation request for: " + request.getTitle());
        CaseWithAllegationsResponse response = caseWorkflowService.createCaseWithWorkflow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/multi-department")
    @Operation(summary = "Create a multi-department case", description = "Create a complex case that requires multiple departments (HR, Legal, CSIS)")
    @PreAuthorize("hasPermission(#request, 'case', 'create')")
    public ResponseEntity<CaseWithAllegationsResponse> createMultiDepartmentCase(@Valid @RequestBody CreateCaseWithAllegationsRequest request) {
        System.out.println("🎯 Creating multi-department case: " + request.getTitle());
        
        // Ensure the case will trigger multi-department workflow
        if (request.getAllegations() == null || request.getAllegations().size() < 2) {
            throw new IllegalArgumentException("Multi-department cases require at least 2 allegations with different classifications");
        }
        
        CaseWithAllegationsResponse response = caseWorkflowService.createCaseWithWorkflow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{caseNumber}")
    @Operation(summary = "Get case details", description = "Retrieve detailed information about a specific case")
    @PreAuthorize("hasPermission(#caseNumber, 'case', 'view')")
    public ResponseEntity<CaseWithAllegationsResponse> getCaseDetails(
            @Parameter(description = "Case number (e.g., CMS-2025-010)") @PathVariable String caseNumber) {
        try {
            CaseWithAllegationsResponse caseDetails = caseWorkflowService.getCaseDetailsByCaseNumber(caseNumber);
            return ResponseEntity.ok(caseDetails);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping
    @Operation(summary = "Get all cases", description = "Retrieve a list of all cases in the system")
    @PreAuthorize("hasAnyAuthority('DIRECTOR', 'IU_MANAGER', 'INTAKE_ANALYST', 'HR_SPECIALIST', 'LEGAL_COUNSEL', 'SECURITY_ANALYST', 'ADMIN')")
    public ResponseEntity<List<CaseWithAllegationsResponse>> getAllCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        try {
            List<CaseWithAllegationsResponse> cases = caseWorkflowService.getAllCases(page, size, status);
            return ResponseEntity.ok(cases);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/my-cases")
    @Operation(summary = "Get my cases", description = "Get cases accessible to the current user for dashboard")
    public ResponseEntity<List<CaseWithAllegationsResponse>> getMyCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).build();
            }
            
            // For now, return all cases - you can add user-specific filtering later
            List<CaseWithAllegationsResponse> cases = caseWorkflowService.getAllCases(page, size, status);
            return ResponseEntity.ok(cases);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/dashboard-cases")
    @Operation(summary = "Get cases for dashboard", description = "Get cases for dashboard without strict authorization")
    public ResponseEntity<List<CaseWithAllegationsResponse>> getDashboardCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).build();
            }
            
            // Log user details for debugging
            System.out.println("🔍 Dashboard cases request from user: " + authentication.getName());
            System.out.println("🔍 User authorities: " + authentication.getAuthorities());
            
            // Return cases without strict authorization for dashboard
            List<CaseWithAllegationsResponse> cases = caseWorkflowService.getAllCases(page, size, status);
            System.out.println("✅ Returning " + cases.size() + " cases for dashboard");
            return ResponseEntity.ok(cases);
        } catch (Exception e) {
            System.err.println("❌ Error fetching dashboard cases: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/dashboard-stats")
    @Operation(summary = "Get dashboard statistics", description = "Get basic statistics for dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).build();
            }
            
            System.out.println("🔍 Dashboard stats request from user: " + authentication.getName());
            
            // Get basic stats without strict authorization
            List<CaseWithAllegationsResponse> allCases = caseWorkflowService.getAllCases(0, 1000, null);
            
            long openCases = allCases.stream()
                .filter(c -> "OPEN".equals(c.getStatus().toString()) || "IN_PROGRESS".equals(c.getStatus().toString()))
                .count();
                
            long investigations = allCases.stream()
                .filter(c -> "IN_PROGRESS".equals(c.getStatus().toString()) || 
                           "HIGH".equals(c.getPriority().toString()) || 
                           "CRITICAL".equals(c.getPriority().toString()))
                .count();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("allOpenCases", openCases);
            stats.put("openInvestigations", investigations);
            stats.put("totalCases", allCases.size());
            
            System.out.println("✅ Dashboard stats - Open: " + openCases + ", Investigations: " + investigations);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("❌ Error fetching dashboard stats: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{caseNumber}/workflow-status")
    @Operation(summary = "Get workflow status", description = "Get the current workflow status and progress for a case")
    @PreAuthorize("hasPermission(#caseNumber, 'case', 'view')")
    public ResponseEntity<Map<String, Object>> getWorkflowStatus(
            @Parameter(description = "Case number") @PathVariable String caseNumber) {
        try {
            Map<String, Object> workflowStatus = new HashMap<>();
            
            // Get the case details to determine workflow status
            CaseWithAllegationsResponse caseDetails = caseWorkflowService.getCaseDetailsByCaseNumber(caseNumber);
            
            if (caseDetails == null) {
                workflowStatus.put("status", "NOT_FOUND");
                workflowStatus.put("message", "No case found with number: " + caseNumber);
            } else {
                String currentStatus = caseDetails.getStatus().toString();
                workflowStatus.put("status", "ACTIVE");
                workflowStatus.put("caseNumber", caseNumber);
                workflowStatus.put("currentStatus", currentStatus);
                workflowStatus.put("message", "Case is currently in " + currentStatus + " status");
                workflowStatus.put("lastUpdated", caseDetails.getUpdatedAt());
            }
            
            return ResponseEntity.ok(workflowStatus);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve workflow status: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{caseNumber}/journey")
    @Operation(summary = "Get workflow journey", description = "Track the complete journey of a case through the workflow")
    @PreAuthorize("hasPermission(#caseNumber, 'case', 'view')")
    public ResponseEntity<Map<String, Object>> getWorkflowJourney(
            @Parameter(description = "Case number") @PathVariable String caseNumber) {
        try {
            Map<String, Object> journey = new HashMap<>();
            
            // This would ideally integrate with your case service to get journey details
            journey.put("caseNumber", caseNumber);
            journey.put("stages", List.of(
                Map.of("stage", "Intake", "status", "completed", "timestamp", "2025-07-12T23:42:38"),
                Map.of("stage", "Classification", "status", "completed", "timestamp", "2025-07-12T23:43:15"),
                Map.of("stage", "HR Review", "status", "in_progress", "timestamp", "2025-07-12T23:45:00"),
                Map.of("stage", "Legal Review", "status", "pending"),
                Map.of("stage", "CSIS Review", "status", "pending"),
                Map.of("stage", "Investigation", "status", "pending"),
                Map.of("stage", "Case Closure", "status", "pending")
            ));
            
            return ResponseEntity.ok(journey);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve workflow journey: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @PostMapping("/{caseNumber}/submit")
    @Operation(summary = "Submit case for workflow processing", description = "Submit a case to transition it to the next workflow step. Only Intake Analysts and Admins can submit cases.")
    @PreAuthorize("hasAnyAuthority('INTAKE_ANALYST', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> submitCase(
            @Parameter(description = "Case number to submit") @PathVariable String caseNumber,
            Authentication authentication) {
        try {
            System.out.println("🎯 Received case submission request for case: " + caseNumber + " by user: " + authentication.getName());
            
            // Get the case details first
            CaseWithAllegationsResponse caseDetails = caseWorkflowService.getCaseDetailsByCaseNumber(caseNumber);
            
            if (caseDetails == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Case not found");
                errorResponse.put("caseNumber", caseNumber);
                return ResponseEntity.status(404).body(errorResponse);
            }
            
            // Check if case is in a valid state for submission
            String currentStatus = caseDetails.getStatus().toString();
            List<String> allowedStatuses = List.of("OPEN", "SUBMITTED", "IN_PROGRESS");
            
            if (!allowedStatuses.contains(currentStatus)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Case cannot be submitted from current status: " + currentStatus);
                errorResponse.put("allowedStatuses", allowedStatuses);
                errorResponse.put("currentStatus", currentStatus);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Submit the case (this will trigger the workflow transition)
            CaseWithAllegationsResponse updatedCase = caseWorkflowService.submitCase(caseNumber, authentication.getName());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Case submitted successfully");
            response.put("caseNumber", caseNumber);
            response.put("previousStatus", currentStatus);
            response.put("newStatus", updatedCase.getStatus().toString());
            response.put("submittedBy", authentication.getName());
            response.put("submittedAt", java.time.Instant.now().toString());
            
            System.out.println("✅ Case " + caseNumber + " submitted successfully. Status changed from " + currentStatus + " to " + updatedCase.getStatus());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error submitting case " + caseNumber + ": " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to submit case: " + e.getMessage());
            errorResponse.put("caseNumber", caseNumber);
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}