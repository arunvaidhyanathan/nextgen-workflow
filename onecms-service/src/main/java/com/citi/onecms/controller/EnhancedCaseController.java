package com.citi.onecms.controller;

import com.citi.onecms.dto.CreateCaseWithAllegationsRequest;
import com.citi.onecms.dto.CaseWithAllegationsResponse;
import com.citi.onecms.dto.QueueTaskResponse;
import com.citi.onecms.service.EnhancedCaseWorkflowService;
import com.citi.onecms.service.QueueTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/cases")
@Tag(name = "Enhanced Case Management", description = "Optimized multi-department case processing")
@RequiredArgsConstructor
public class EnhancedCaseController {
    
    private final EnhancedCaseWorkflowService enhancedCaseWorkflowService;
    private final QueueTaskService queueTaskService;
    
    @PostMapping("/create-multi-department")
    @Operation(summary = "Create multi-department case", 
               description = "Creates case with automatic department routing, queue population, and workflow initiation")
    public ResponseEntity<CaseWithAllegationsResponse> createMultiDepartmentCase(
            @Valid @RequestBody CreateCaseWithAllegationsRequest request,
            @Parameter(description = "Routing strategy: AUTO, HR_PRIORITY, SECURITY_PRIORITY, LEGAL_PRIORITY") 
            @RequestParam(defaultValue = "AUTO") String routingStrategy) {
        
        try {
            // This will handle:
            // 1. Department classification via enhanced DMN
            // 2. Workflow metadata creation/update
            // 3. Queue task population
            // 4. Multi-department process initiation
            CaseWithAllegationsResponse response = enhancedCaseWorkflowService
                    .createMultiDepartmentCase(request, routingStrategy);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/department/{department}/my-tasks")
    @Operation(summary = "Get department-specific tasks for current user",
               description = "Returns tasks from department queues accessible to the user's role")
    public ResponseEntity<List<QueueTaskResponse>> getDepartmentTasks(
            @Parameter(description = "Department: HR, CSIS, LEGAL, INVESTIGATION, DIRECTOR") 
            @PathVariable String department,
            @Parameter(description = "Show only unassigned tasks") 
            @RequestParam(defaultValue = "false") boolean unassignedOnly,
            Authentication authentication) {
        
        try {
            List<QueueTaskResponse> tasks = enhancedCaseWorkflowService
                    .getTasksForUserByDepartment(authentication.getName(), department, unassignedOnly);
            
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.status(403).build();
        }
    }
    
    @GetMapping("/my-dashboard")
    @Operation(summary = "Get user's multi-department dashboard",
               description = "Returns all accessible tasks across departments for the current user")
    public ResponseEntity<Map<String, List<QueueTaskResponse>>> getMyDashboard(
            Authentication authentication) {
        
        try {
            Map<String, List<QueueTaskResponse>> dashboard = enhancedCaseWorkflowService
                    .getUserDashboard(authentication.getName());
            
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/queue/claim-next")
    @Operation(summary = "Claim next available task from user's accessible queues",
               description = "Automatically claims the highest priority task from queues accessible to user's role")
    public ResponseEntity<QueueTaskResponse> claimNextAvailableTask(
            @Parameter(description = "Preferred department: HR, CSIS, LEGAL, INVESTIGATION") 
            @RequestParam(required = false) String preferredDepartment,
            Authentication authentication) {
        
        try {
            QueueTaskResponse task = enhancedCaseWorkflowService
                    .claimNextTaskForUser(authentication.getName(), preferredDepartment);
            
            if (task != null) {
                return ResponseEntity.ok(task);
            } else {
                return ResponseEntity.noContent().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/queue/tasks/{taskId}/escalate")
    @Operation(summary = "Escalate task to higher authority",
               description = "Escalates task based on department hierarchy and user role")
    public ResponseEntity<QueueTaskResponse> escalateTask(
            @Parameter(description = "Task ID to escalate") @PathVariable String taskId,
            @Parameter(description = "Escalation reason") @RequestParam String reason,
            Authentication authentication) {
        
        try {
            QueueTaskResponse escalatedTask = enhancedCaseWorkflowService
                    .escalateTask(taskId, reason, authentication.getName());
            
            return ResponseEntity.ok(escalatedTask);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/analytics/department-workload")
    @Operation(summary = "Get department workload analytics",
               description = "Returns workload distribution and performance metrics across all departments")
    public ResponseEntity<Map<String, Object>> getDepartmentWorkloadAnalytics() {
        
        try {
            Map<String, Object> analytics = enhancedCaseWorkflowService
                    .getDepartmentWorkloadAnalytics();
            
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}