package com.citi.onecms.controller;

import com.citi.onecms.dto.CreateCaseWithAllegationsRequest;
import com.citi.onecms.dto.CaseWithAllegationsResponse;
import com.citi.onecms.service.AuthorizationService;
import com.citi.onecms.service.CaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cms/v1")
@Tag(name = "Case Management", description = "APIs for managing cases and workflow processes")
@Slf4j
public class CaseManagementController {
    
    @Autowired
    private CaseService caseService;
    
    @Autowired
    private AuthorizationService authorizationService;
    
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Case management controller is working!");
    }
    
    @GetMapping("/auth-test")
    @Operation(summary = "Test authentication", description = "Test endpoint to verify session-based authentication")
    public ResponseEntity<Map<String, Object>> testAuth(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        String userId = authorizationService.extractUserId(request);
        
        if (!authorizationService.validateUserPresence(userId)) {
            response.put("authenticated", false);
            response.put("message", "No X-User-Id header found or invalid");
            return ResponseEntity.status(401).body(response);
        }
        
        response.put("authenticated", true);
        response.put("userId", userId);
        response.put("authMethod", "session-based");
        
        // Test authorization for different actions
        response.put("canCreateCase", authorizationService.checkCaseAuthorization(userId, null, "create"));
        response.put("canViewCases", authorizationService.checkCaseAuthorization(userId, "test-case", "view"));
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/createcase")
    @Operation(summary = "Create a new case", description = "Create a new case with allegations and start the workflow process")
    public ResponseEntity<CaseWithAllegationsResponse> createCase(
            @Valid @RequestBody CreateCaseWithAllegationsRequest request,
            HttpServletRequest httpRequest) {
        String userId = authorizationService.extractUserId(httpRequest);
        
        if (!authorizationService.validateUserPresence(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Check authorization to create cases
        if (!authorizationService.checkCaseAuthorization(userId, null, "create")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        log.info("🎯 Received case creation request for: {} by user: {}", request.getTitle(), userId);
        
        // CreatedBy will be set in the service using userId
        
        CaseWithAllegationsResponse response = caseService.createCase(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/createcase/multi-department")
    @Operation(summary = "Create a multi-department case", description = "Create a complex case that requires multiple departments (HR, Legal, CSIS)")
    public ResponseEntity<CaseWithAllegationsResponse> createMultiDepartmentCase(
            @Valid @RequestBody CreateCaseWithAllegationsRequest request,
            HttpServletRequest httpRequest) {
        String userId = authorizationService.extractUserId(httpRequest);
        
        if (!authorizationService.validateUserPresence(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Check authorization to create cases
        if (!authorizationService.checkCaseAuthorization(userId, null, "create")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        log.info("🎯 Creating multi-department case: {} by user: {}", request.getTitle(), userId);
        
        // Ensure the case will trigger multi-department workflow
        if (request.getAllegations() == null || request.getAllegations().size() < 2) {
            throw new IllegalArgumentException("Multi-department cases require at least 2 allegations with different classifications");
        }
        
        // CreatedBy will be set in the service using userId
        
        CaseWithAllegationsResponse response = caseService.createCase(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/cases/{caseNumber}")
    @Operation(summary = "Get case details", description = "Retrieve detailed information about a specific case")
    public ResponseEntity<CaseWithAllegationsResponse> getCaseDetails(
            @Parameter(description = "Case number (e.g., CMS-2025-010)") @PathVariable String caseNumber,
            HttpServletRequest httpRequest) {
        String userId = authorizationService.extractUserId(httpRequest);
        
        if (!authorizationService.validateUserPresence(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Check authorization to view this case
        if (!authorizationService.checkCaseAuthorization(userId, caseNumber, "view")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            CaseWithAllegationsResponse caseDetails = caseService.getCaseByNumber(caseNumber);
            return ResponseEntity.ok(caseDetails);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/cases")
    @Operation(summary = "Get all cases", description = "Retrieve a list of all cases in the system")
    public ResponseEntity<List<CaseWithAllegationsResponse>> getAllCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            HttpServletRequest httpRequest) {
        String userId = authorizationService.extractUserId(httpRequest);
        
        if (!authorizationService.validateUserPresence(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Check authorization to list cases
        if (!authorizationService.checkCaseAuthorization(userId, "all", "list")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            List<CaseWithAllegationsResponse> cases = caseService.getAllCases(page, size, status);
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
            HttpServletRequest httpRequest) {
        String userId = authorizationService.extractUserId(httpRequest);
        
        if (!authorizationService.validateUserPresence(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            List<CaseWithAllegationsResponse> cases = caseService.getAllCases(page, size, status);
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
            HttpServletRequest httpRequest) {
        String userId = authorizationService.extractUserId(httpRequest);
        
        if (!authorizationService.validateUserPresence(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            log.info("Dashboard cases request from user: {}", userId);
            
            List<CaseWithAllegationsResponse> cases = caseService.getAllCases(page, size, status);
            log.info("Returning {} cases for dashboard", cases.size());
            return ResponseEntity.ok(cases);
        } catch (Exception e) {
            log.error("Error fetching dashboard cases: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/dashboard-stats")
    @Operation(summary = "Get dashboard statistics", description = "Get basic statistics for dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats(HttpServletRequest httpRequest) {
        String userId = authorizationService.extractUserId(httpRequest);
        
        if (!authorizationService.validateUserPresence(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            log.info("Dashboard stats request from user: {}", userId);
            
            List<CaseWithAllegationsResponse> allCases = caseService.getAllCases(0, 1000, null);
            
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
            
            log.info("Dashboard stats - Open: {}, Investigations: {}", openCases, investigations);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching dashboard stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{caseNumber}/workflow-status")
    @Operation(summary = "Get workflow status", description = "Get the current workflow status and progress for a case")
    public ResponseEntity<Map<String, Object>> getWorkflowStatus(
            @Parameter(description = "Case number") @PathVariable String caseNumber) {
        try {
            Map<String, Object> workflowStatus = new HashMap<>();
            
            // Get the case details to determine workflow status
            CaseWithAllegationsResponse caseDetails = caseService.getCaseByNumber(caseNumber);
            
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
    public ResponseEntity<Map<String, Object>> submitCase(
            @Parameter(description = "Case number to submit") @PathVariable String caseNumber,
            HttpServletRequest httpRequest) {
        String userId = authorizationService.extractUserId(httpRequest);
        
        if (!authorizationService.validateUserPresence(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Check authorization to submit cases
        if (!authorizationService.checkCaseAuthorization(userId, caseNumber, "submit")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            log.info("Received case submission request for case: {} by user: {}", caseNumber, userId);
            
            // Get the case details first
            CaseWithAllegationsResponse caseDetails = caseService.getCaseByNumber(caseNumber);
            
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
            CaseWithAllegationsResponse updatedCase = caseService.submitCase(caseNumber, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Case submitted successfully");
            response.put("caseNumber", caseNumber);
            response.put("previousStatus", currentStatus);
            response.put("newStatus", updatedCase.getStatus().toString());
            response.put("submittedBy", userId);
            response.put("submittedAt", java.time.Instant.now().toString());
            
            log.info("Case {} submitted successfully. Status changed from {} to {}", caseNumber, currentStatus, updatedCase.getStatus());
            
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