package com.citi.onecms.controller;

import com.citi.onecms.dto.CreateCaseWithAllegationsRequest;
import com.citi.onecms.dto.CaseWithAllegationsResponse;
import com.citi.onecms.dto.CreateCaseDraftRequest;
import com.citi.onecms.dto.CaseDraftResponse;
import com.citi.onecms.dto.EnhancedCreateCaseRequest;
import com.citi.onecms.dto.workflow.TaskResponse;
import com.citi.onecms.service.AuthorizationService;
import com.citi.onecms.service.CaseService;
import com.citi.onecms.service.CaseDraftService;
import com.citi.onecms.client.WorkflowServiceClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cms/v1")
@Tag(name = "Case Management", description = "APIs for managing cases and workflow processes in the OneCMS system")
@Slf4j
public class CaseManagementController {
    
    @Autowired
    private CaseService caseService;
    
    @Autowired
    private CaseDraftService caseDraftService;
    
    @Autowired
    private AuthorizationService authorizationService;
    
    @Autowired
    private WorkflowServiceClient workflowServiceClient;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Case management controller is working!");
    }
    
    @PostMapping("/test-enhance")
    public ResponseEntity<CaseWithAllegationsResponse> testEnhanceCase(
            @Valid @RequestBody EnhancedCreateCaseRequest request,
            HttpServletRequest httpRequest) {
        String userId = authorizationService.extractUserId(httpRequest);
        if (userId == null) userId = "alice.intake"; // Default for testing
        
        log.info("🧪 TEST: Enhancing case {} by user {}", request.getCaseId(), userId);
        
        try {
            CaseWithAllegationsResponse response = caseService.enhanceExistingCase(request, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Case not found")) {
                return ResponseEntity.notFound().build();
            }
            log.error("❌ Test enhance failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
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
    
    @PostMapping("/createcase-draft")
    @Operation(summary = "Create a case draft", 
               description = "Create a case draft that starts the workflow process and captures initial task information. This creates both a case entity and a case draft with workflow integration.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Case draft created successfully",
                    content = @Content(mediaType = "application/json", 
                              schema = @Schema(implementation = CaseDraftResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data or validation errors",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Missing or invalid user authentication header",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "User not authorized to create case drafts",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error during case draft creation",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<CaseDraftResponse> createCaseDraft(
            @Valid @RequestBody CreateCaseDraftRequest request,
            HttpServletRequest httpRequest) {
        
        String userId = authorizationService.extractUserId(httpRequest);
        
        if (!authorizationService.validateUserPresence(userId)) {
            log.warn("Case draft creation attempted without valid user authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Check authorization to create case drafts (use same permission as creating cases)
        if (!authorizationService.checkCaseAuthorization(userId, null, "create")) {
            log.warn("User {} not authorized to create case drafts", userId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        log.info("🎯 Creating case draft: {} by user: {}", request.getCaseDraftTitle(), userId);
        
        try {
            CaseDraftResponse response = caseDraftService.createCaseDraft(request, userId);
            
            log.info("✅ Case draft created successfully: CaseId={}, ProcessInstanceId={}", 
                    response.getCaseId(), response.getProcessInstanceId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("❌ Failed to create case draft for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/case-drafts")
    @Operation(summary = "Get case drafts for current user", 
               description = "Retrieve all case drafts created by the current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Case drafts retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid user authentication header"),
        @ApiResponse(responseCode = "403", description = "User not authorized to view case drafts")
    })
    public ResponseEntity<List<CaseDraftResponse>> getCaseDrafts(HttpServletRequest httpRequest) {
        
        String userId = authorizationService.extractUserId(httpRequest);
        
        if (!authorizationService.validateUserPresence(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Check authorization to view case drafts
        if (!authorizationService.checkCaseAuthorization(userId, null, "view")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        log.debug("Retrieving case drafts for user: {}", userId);
        
        List<CaseDraftResponse> drafts = caseDraftService.getCaseDraftsByUser(userId);
        return ResponseEntity.ok(drafts);
    }
    
    @GetMapping("/case-drafts/{caseDraftId}")
    @Operation(summary = "Get case draft by ID", 
               description = "Retrieve a specific case draft by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Case draft found"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid user authentication header"),
        @ApiResponse(responseCode = "403", description = "User not authorized to view this case draft"),
        @ApiResponse(responseCode = "404", description = "Case draft not found")
    })
    public ResponseEntity<CaseDraftResponse> getCaseDraftById(
            @Parameter(description = "Case draft ID", required = true) 
            @PathVariable Long caseDraftId,
            HttpServletRequest httpRequest) {
        
        String userId = authorizationService.extractUserId(httpRequest);
        
        if (!authorizationService.validateUserPresence(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        log.debug("Retrieving case draft {} for user: {}", caseDraftId, userId);
        
        return caseDraftService.getCaseDraftById(caseDraftId)
                .map(draft -> {
                    // Check if user can view this draft (only creator for now)
                    if (!draft.getCreatedByUserId().equals(userId)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<CaseDraftResponse>build();
                    }
                    return ResponseEntity.ok(draft);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/case-drafts/{caseDraftId}/submit")
    @Operation(summary = "Submit case draft", 
               description = "Submit a case draft for processing (mark as submitted)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Case draft submitted successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid user authentication header"),
        @ApiResponse(responseCode = "403", description = "User not authorized to submit this case draft"),
        @ApiResponse(responseCode = "404", description = "Case draft not found")
    })
    public ResponseEntity<CaseDraftResponse> submitCaseDraft(
            @Parameter(description = "Case draft ID", required = true) 
            @PathVariable Long caseDraftId,
            HttpServletRequest httpRequest) {
        
        String userId = authorizationService.extractUserId(httpRequest);
        
        if (!authorizationService.validateUserPresence(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        log.info("Submitting case draft {} by user: {}", caseDraftId, userId);
        
        try {
            CaseDraftResponse response = caseDraftService.submitCaseDraft(caseDraftId, userId);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("not authorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            log.error("Failed to submit case draft {}: {}", caseDraftId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/createcase")
    @Operation(summary = "Create a new case", 
               description = "Create a comprehensive case with allegations, entities, and narratives. Automatically starts the associated workflow process.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Case created successfully",
                    content = @Content(mediaType = "application/json", 
                              schema = @Schema(implementation = CaseWithAllegationsResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data or validation errors",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Missing or invalid user authentication header",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "User not authorized to create cases",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error during case creation",
                    content = @Content(mediaType = "application/json"))
    })
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
    
    @PostMapping("/enhance-case")
    @Operation(summary = "Enhance existing case", 
               description = "Enhance an existing case draft with allegations, entities, and narratives (Option 1 two-phase approach).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Case enhanced successfully",
                    content = @Content(mediaType = "application/json", 
                              schema = @Schema(implementation = CaseWithAllegationsResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data or validation errors"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid user authentication header"),
        @ApiResponse(responseCode = "403", description = "User not authorized to enhance cases"),
        @ApiResponse(responseCode = "404", description = "Case not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error during case enhancement")
    })
    public ResponseEntity<CaseWithAllegationsResponse> enhanceCase(
            @Valid @RequestBody EnhancedCreateCaseRequest request,
            HttpServletRequest httpRequest) {
        String userId = authorizationService.extractUserId(httpRequest);
        
        if (!authorizationService.validateUserPresence(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Check authorization to enhance cases (same permission as creating cases)
        if (!authorizationService.checkCaseAuthorization(userId, null, "create")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        log.info("🔄 Received case enhancement request for case_id: {} by user: {}", 
                 request.getCaseId(), userId);
        
        try {
            CaseWithAllegationsResponse response = caseService.enhanceExistingCase(request, userId);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Case not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            log.error("❌ Failed to enhance case for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
            @Parameter(description = "Case number") @PathVariable String caseNumber,
            HttpServletRequest httpRequest) {
        String userId = authorizationService.extractUserId(httpRequest);
        
        if (!authorizationService.validateUserPresence(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Check authorization to view workflow status
        if (!authorizationService.checkWorkflowStatusAuthorization(userId, caseNumber, "view")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
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
            @Parameter(description = "Case number") @PathVariable String caseNumber,
            HttpServletRequest httpRequest) {
        String userId = authorizationService.extractUserId(httpRequest);
        
        if (!authorizationService.validateUserPresence(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Check authorization to view workflow journey
        if (!authorizationService.checkWorkflowStatusAuthorization(userId, caseNumber, "view")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
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
        
        // Check authorization to submit cases (workflow transition)
        if (!authorizationService.checkCaseSubmissionAuthorization(userId, caseNumber)) {
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
    
    // ========================================
    // WORKFLOW TASK MANAGEMENT ENDPOINTS
    // ========================================
    
    @GetMapping("/tasks/my-tasks")
    @Operation(summary = "Get my tasks", description = "Get workflow tasks assigned to the current user")
    public ResponseEntity<List<TaskResponse>> getMyTasks(HttpServletRequest httpRequest) {
        String userId = authorizationService.extractUserId(httpRequest);
        
        if (!authorizationService.validateUserPresence(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Check authorization to view own tasks
        if (!authorizationService.checkTaskAuthorization(userId, null, "list")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            List<TaskResponse> tasks = workflowServiceClient.getUserTasks(userId);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            log.error("Failed to get tasks for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/tasks/queue/{queueName}")
    @Operation(summary = "Get tasks by queue", description = "Get workflow tasks in a specific queue")
    public ResponseEntity<List<TaskResponse>> getTasksByQueue(
            @Parameter(description = "Queue name") @PathVariable String queueName,
            @RequestParam(defaultValue = "false") boolean unassignedOnly,
            HttpServletRequest httpRequest) {
        String userId = authorizationService.extractUserId(httpRequest);
        
        if (!authorizationService.validateUserPresence(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Check authorization to view tasks in this queue
        if (!authorizationService.checkQueueAuthorization(userId, queueName, "view")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            List<TaskResponse> tasks = workflowServiceClient.getTasksByQueue(queueName, unassignedOnly, userId);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            log.error("Failed to get tasks for queue {}: {}", queueName, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/tasks/{taskId}/claim")
    @Operation(summary = "Claim a task", description = "Claim a workflow task for the current user")
    public ResponseEntity<TaskResponse> claimTask(
            @Parameter(description = "Task ID") @PathVariable String taskId,
            HttpServletRequest httpRequest) {
        String userId = authorizationService.extractUserId(httpRequest);
        
        if (!authorizationService.validateUserPresence(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Check authorization to claim this task
        if (!authorizationService.checkTaskAuthorization(userId, taskId, "claim")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            TaskResponse task = workflowServiceClient.claimTask(taskId, userId);
            return ResponseEntity.ok(task);
        } catch (Exception e) {
            log.error("Failed to claim task {} for user {}: {}", taskId, userId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/tasks/{taskId}/complete")
    @Operation(summary = "Complete a task", description = "Complete a workflow task with optional variables")
    public ResponseEntity<Map<String, Object>> completeTask(
            @Parameter(description = "Task ID") @PathVariable String taskId,
            @RequestBody(required = false) Map<String, Object> variables,
            HttpServletRequest httpRequest) {
        String userId = authorizationService.extractUserId(httpRequest);
        
        if (!authorizationService.validateUserPresence(userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Check authorization to complete this task
        if (!authorizationService.checkTaskAuthorization(userId, taskId, "complete")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            workflowServiceClient.completeTask(taskId, variables, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Task completed successfully");
            response.put("taskId", taskId);
            response.put("completedBy", userId);
            response.put("completedAt", java.time.Instant.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to complete task {} for user {}: {}", taskId, userId, e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to complete task: " + e.getMessage());
            errorResponse.put("taskId", taskId);
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    // ========================================
    // HELPER METHODS FOR REQUEST CONVERSION
    // ========================================
    
    /**
     * Convert Map request body to EnhancedCreateCaseRequest for two-phase approach
     */
    private EnhancedCreateCaseRequest convertToEnhancedRequest(Map<String, Object> requestBody) {
        try {
            // Validate required fields for enhancement
            if (!requestBody.containsKey("case_id") || requestBody.get("case_id") == null) {
                throw new IllegalArgumentException("case_id is required for case enhancement");
            }
            
            EnhancedCreateCaseRequest request = objectMapper.convertValue(requestBody, EnhancedCreateCaseRequest.class);
            
            // Additional validation
            if (request.getCaseId() == null || request.getCaseId().trim().isEmpty()) {
                throw new IllegalArgumentException("case_id cannot be empty");
            }
            
            return request;
        } catch (Exception e) {
            log.error("Failed to convert request to EnhancedCreateCaseRequest: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid request format for case enhancement: " + e.getMessage());
        }
    }
    
    /**
     * Convert Map request body to CreateCaseWithAllegationsRequest for original flow
     */
    private CreateCaseWithAllegationsRequest convertToCreateRequest(Map<String, Object> requestBody) {
        try {
            CreateCaseWithAllegationsRequest request = objectMapper.convertValue(requestBody, CreateCaseWithAllegationsRequest.class);
            
            // Validate required fields for new case creation
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("Title is required for case creation");
            }
            if (request.getComplainantName() == null || request.getComplainantName().trim().isEmpty()) {
                throw new IllegalArgumentException("Complainant name is required for case creation");
            }
            if (request.getReporterId() == null || request.getReporterId().trim().isEmpty()) {
                throw new IllegalArgumentException("Reporter ID is required for case creation");
            }
            
            return request;
        } catch (Exception e) {
            log.error("Failed to convert request to CreateCaseWithAllegationsRequest: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid request format for case creation: " + e.getMessage());
        }
    }
}