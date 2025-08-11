package com.citi.onecms.service;

import com.citi.onecms.dto.CreateCaseWithAllegationsRequest;
import com.citi.onecms.dto.RegisterWorkflowMetadataRequest;
import com.citi.onecms.dto.CaseWithAllegationsResponse;
import com.citi.onecms.dto.QueueTaskResponse;
import com.citi.onecms.enums.TaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EnhancedCaseWorkflowService {
    
    private final CaseWorkflowService caseWorkflowService;
    private final QueueTaskService queueTaskService;
    private final WorkflowMetadataService workflowMetadataService;
    
    // Department-specific queue mappings
    private static final Map<String, Set<String>> ROLE_TO_QUEUES = Map.of(
        "HR_SPECIALIST", Set.of("hr-intake-queue", "hr-processing-queue"),
        "CSIS_ANALYST", Set.of("csis-intake-queue", "csis-investigation-queue"),
        "SECURITY_ANALYST", Set.of("csis-intake-queue", "csis-investigation-queue"),
        "LEGAL_COUNSEL", Set.of("legal-intake-queue", "legal-review-queue"),
        "INVESTIGATOR", Set.of("investigation-queue", "field-investigation-queue"),
        "INVESTIGATION_MANAGER", Set.of("investigation-queue", "field-investigation-queue", "investigation-approval-queue"),
        "DIRECTOR", Set.of("director-oversight-queue", "hr-approval-queue", "csis-approval-queue", "legal-approval-queue")
    );
    
    private static final Map<String, String> DEPARTMENT_TO_QUEUE = Map.of(
        "HR", "hr-intake-queue",
        "CSIS", "csis-intake-queue", 
        "LEGAL", "legal-intake-queue",
        "INVESTIGATION", "investigation-queue",
        "DIRECTOR", "director-oversight-queue"
    );
    
    /**
     * Enhanced multi-department case creation with automatic workflow setup
     */
    public CaseWithAllegationsResponse createMultiDepartmentCase(
            CreateCaseWithAllegationsRequest request, String routingStrategy) {
        
        log.info("Creating multi-department case with routing strategy: {}", routingStrategy);
        
        try {
            // Step 1: Ensure workflow metadata exists and is up-to-date
            ensureWorkflowMetadataExists();
            
            // Step 2: Create case using existing service (this handles DMN classification)
            CaseWithAllegationsResponse response = caseWorkflowService.createCaseWithWorkflow(request);
            
            // Step 3: Apply routing strategy optimizations if needed
            if (!"AUTO".equals(routingStrategy)) {
                applyRoutingStrategy(response, routingStrategy);
            }
            
            log.info("Successfully created multi-department case: {}", response.getCaseNumber());
            return response;
            
        } catch (Exception e) {
            log.error("Failed to create multi-department case: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create multi-department case", e);
        }
    }
    
    /**
     * Get tasks for user based on their role and department access
     */
    public List<QueueTaskResponse> getTasksForUserByDepartment(
            String username, String department, boolean unassignedOnly) {
        
        // Get user's role (this would normally come from authentication service)
        String userRole = getUserRole(username);
        
        // Check if user has access to this department
        String queueName = DEPARTMENT_TO_QUEUE.get(department.toUpperCase());
        if (queueName == null) {
            throw new IllegalArgumentException("Invalid department: " + department);
        }
        
        Set<String> accessibleQueues = ROLE_TO_QUEUES.getOrDefault(userRole, Set.of());
        if (!accessibleQueues.contains(queueName)) {
            throw new SecurityException("User " + username + " does not have access to " + department + " queue");
        }
        
        // Get tasks from the department queue
        return queueTaskService.getTasksByQueue(queueName, unassignedOnly);
    }
    
    /**
     * Get comprehensive dashboard for user across all accessible departments
     */
    public Map<String, List<QueueTaskResponse>> getUserDashboard(String username) {
        String userRole = getUserRole(username);
        Set<String> accessibleQueues = ROLE_TO_QUEUES.getOrDefault(userRole, Set.of());
        
        Map<String, List<QueueTaskResponse>> dashboard = new HashMap<>();
        
        for (String queueName : accessibleQueues) {
            try {
                List<QueueTaskResponse> tasks = queueTaskService.getTasksByQueue(queueName, false);
                dashboard.put(queueName, tasks);
            } catch (Exception e) {
                log.warn("Failed to load tasks from queue {}: {}", queueName, e.getMessage());
                dashboard.put(queueName, new ArrayList<>());
            }
        }
        
        return dashboard;
    }
    
    /**
     * Claim next highest priority task from user's accessible queues
     */
    public QueueTaskResponse claimNextTaskForUser(String username, String preferredDepartment) {
        String userRole = getUserRole(username);
        Set<String> accessibleQueues = ROLE_TO_QUEUES.getOrDefault(userRole, Set.of());
        
        // If preferred department is specified, try that first
        if (preferredDepartment != null) {
            String preferredQueue = DEPARTMENT_TO_QUEUE.get(preferredDepartment.toUpperCase());
            if (preferredQueue != null && accessibleQueues.contains(preferredQueue)) {
                QueueTaskResponse task = queueTaskService.getNextTaskFromQueue(preferredQueue);
                if (task != null) {
                    return queueTaskService.claimTask(task.getTaskId(), username);
                }
            }
        }
        
        // Try all accessible queues in priority order
        List<String> prioritizedQueues = prioritizeQueues(accessibleQueues);
        
        for (String queueName : prioritizedQueues) {
            QueueTaskResponse task = queueTaskService.getNextTaskFromQueue(queueName);
            if (task != null) {
                return queueTaskService.claimTask(task.getTaskId(), username);
            }
        }
        
        return null; // No tasks available
    }
    
    /**
     * Escalate task based on department hierarchy
     */
    public QueueTaskResponse escalateTask(String taskId, String reason, String username) {
        QueueTaskResponse task = queueTaskService.getQueueTask(taskId);
        
        // Determine escalation queue based on current queue
        String escalationQueue = determineEscalationQueue(task.getQueueName());
        
        if (escalationQueue != null) {
            // Update task queue and add escalation note
            // This would require enhancing QueueTaskService to support queue changes
            log.info("Escalating task {} from {} to {} by user {} - Reason: {}", 
                    taskId, task.getQueueName(), escalationQueue, username, reason);
            
            // For now, unclaim the task so it can be picked up by higher authority
            return queueTaskService.unclaimTask(taskId);
        }
        
        throw new IllegalStateException("No escalation path available for queue: " + task.getQueueName());
    }
    
    /**
     * Get department workload analytics
     */
    public Map<String, Object> getDepartmentWorkloadAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        
        // Get statistics for each department queue
        for (Map.Entry<String, String> entry : DEPARTMENT_TO_QUEUE.entrySet()) {
            String department = entry.getKey();
            String queueName = entry.getValue();
            
            try {
                Map<String, Object> queueStats = queueTaskService.getQueueStatistics(queueName);
                analytics.put(department, queueStats);
            } catch (Exception e) {
                log.warn("Failed to get statistics for queue {}: {}", queueName, e.getMessage());
            }
        }
        
        // Add overall metrics
        analytics.put("totalQueues", DEPARTMENT_TO_QUEUE.size());
        analytics.put("timestamp", System.currentTimeMillis());
        
        return analytics;
    }
    
    /**
     * Ensure workflow metadata exists for the CMS process
     */
    private void ensureWorkflowMetadataExists() {
        String processKey = "Process_CMS_Workflow_Updated";
        
        try {
            // Try to get existing metadata
            workflowMetadataService.getWorkflowMetadata(processKey);
            log.debug("Workflow metadata already exists for process: {}", processKey);
        } catch (Exception e) {
            // Create metadata if it doesn't exist
            log.info("Creating workflow metadata for process: {}", processKey);
            
            RegisterWorkflowMetadataRequest metadataRequest = RegisterWorkflowMetadataRequest.builder()
                    .processDefinitionKey(processKey)
                    .processName("CMS Multi-Department Case Management Workflow")
                    .description("Enhanced workflow for multi-department case processing with queue-based task distribution")
                    .candidateGroupMappings(Map.of(
                        "hr", "hr-intake-queue",
                        "legal", "legal-intake-queue",
                        "csis", "csis-intake-queue",
                        "security", "csis-intake-queue",
                        "investigator", "investigation-queue",
                        "director", "director-oversight-queue",
                        "default", "default-queue"
                    ))
                    .createdBy("system")
                    .build();
            
            workflowMetadataService.registerWorkflowMetadata(metadataRequest);
        }
    }
    
    /**
     * Apply specific routing strategy optimizations
     */
    private void applyRoutingStrategy(CaseWithAllegationsResponse response, String strategy) {
        switch (strategy) {
            case "HR_PRIORITY":
                // Boost priority for HR tasks
                log.info("Applying HR priority routing for case: {}", response.getCaseNumber());
                break;
            case "SECURITY_PRIORITY":
                // Boost priority for CSIS tasks  
                log.info("Applying security priority routing for case: {}", response.getCaseNumber());
                break;
            case "LEGAL_PRIORITY":
                // Boost priority for legal tasks
                log.info("Applying legal priority routing for case: {}", response.getCaseNumber());
                break;
        }
    }
    
    /**
     * Get user role - this would integrate with your authentication service
     */
    private String getUserRole(String username) {
        // This is a placeholder - integrate with your actual user service
        // For now, determine role based on username patterns or default mapping
        if (username.contains("hr")) return "HR_SPECIALIST";
        if (username.contains("legal")) return "LEGAL_COUNSEL";
        if (username.contains("security") || username.contains("csis")) return "SECURITY_ANALYST";
        if (username.contains("investigator")) return "INVESTIGATOR";
        if (username.contains("director")) return "DIRECTOR";
        
        return "HR_SPECIALIST"; // Default fallback
    }
    
    /**
     * Prioritize queues based on business rules
     */
    private List<String> prioritizeQueues(Set<String> queues) {
        return queues.stream()
                .sorted((q1, q2) -> {
                    // Priority order: director > investigation > csis > legal > hr
                    return getQueuePriority(q2) - getQueuePriority(q1);
                })
                .collect(Collectors.toList());
    }
    
    private int getQueuePriority(String queueName) {
        if (queueName.contains("director")) return 5;
        if (queueName.contains("investigation")) return 4;
        if (queueName.contains("csis")) return 3;
        if (queueName.contains("legal")) return 2;
        if (queueName.contains("hr")) return 1;
        return 0;
    }
    
    /**
     * Determine escalation queue based on current queue
     */
    private String determineEscalationQueue(String currentQueue) {
        if (currentQueue.contains("hr") && !currentQueue.contains("approval")) {
            return "hr-approval-queue";
        }
        if (currentQueue.contains("csis") && !currentQueue.contains("approval")) {
            return "csis-approval-queue";
        }
        if (currentQueue.contains("legal") && !currentQueue.contains("approval")) {
            return "legal-approval-queue";
        }
        if (currentQueue.contains("investigation") && !currentQueue.contains("approval")) {
            return "investigation-approval-queue";
        }
        
        // Final escalation to director
        return "director-oversight-queue";
    }
}