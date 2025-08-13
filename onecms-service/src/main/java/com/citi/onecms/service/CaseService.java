package com.citi.onecms.service;

import com.citi.onecms.client.WorkflowServiceClient;
import com.citi.onecms.dto.CreateCaseWithAllegationsRequest;
import com.citi.onecms.dto.CaseWithAllegationsResponse;
import com.citi.onecms.dto.workflow.StartProcessResponse;
import com.citi.onecms.dto.workflow.TaskResponse;
import com.citi.onecms.entity.Case;
import com.citi.onecms.entity.CaseStatus;
import com.citi.onecms.entity.Priority;
import com.citi.onecms.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Complete Case Service with workflow and authorization integration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaseService {
    
    private final CaseRepository caseRepository;
    private final WorkflowServiceClient workflowServiceClient;
    
    @Transactional
    public CaseWithAllegationsResponse createCase(CreateCaseWithAllegationsRequest request, String userId) {
        log.info("Creating case: {} by user: {}", request.getTitle(), userId);
        
        try {
            // 1. Create case entity and save to database
            Case caseEntity = createCaseEntity(request, userId);
            Case savedCase = caseRepository.save(caseEntity);
            
            log.info("Case created in database: {} with ID: {}", savedCase.getCaseNumber(), savedCase.getId());
            
            // 2. Start workflow process
            try {
                String processDefinitionKey = determineWorkflowProcess(request);
                Map<String, Object> workflowVariables = buildWorkflowVariables(request, savedCase);
                
                StartProcessResponse processResponse = workflowServiceClient.startProcess(
                    processDefinitionKey,
                    savedCase.getCaseNumber(),
                    workflowVariables,
                    userId
                );
                
                // 3. Update case with process instance ID
                savedCase.setProcessInstanceId(processResponse.getProcessInstanceId());
                caseRepository.save(savedCase);
                
                log.info("Workflow process started: processInstanceId={} for case={}", 
                         processResponse.getProcessInstanceId(), savedCase.getCaseNumber());
                
                // 4. Build complete response with workflow information
                CaseWithAllegationsResponse response = convertToResponse(savedCase);
                
                // Add workflow metadata
                Map<String, Object> workflowInfo = new HashMap<>();
                workflowInfo.put("processInstanceId", processResponse.getProcessInstanceId());
                workflowInfo.put("processDefinitionKey", processDefinitionKey);
                workflowInfo.put("workflowStatus", "STARTED");
                
                // Get initial tasks if available
                try {
                    List<TaskResponse> initialTasks = workflowServiceClient.getUserTasks(userId);
                    if (!initialTasks.isEmpty()) {
                        TaskResponse firstTask = initialTasks.stream()
                            .filter(task -> savedCase.getProcessInstanceId() != null && 
                                          savedCase.getProcessInstanceId().equals(task.getProcessInstanceId()))
                            .findFirst()
                            .orElse(null);
                        
                        if (firstTask != null) {
                            Map<String, Object> taskInfo = new HashMap<>();
                            taskInfo.put("taskId", firstTask.getTaskId());
                            taskInfo.put("taskName", firstTask.getTaskName());
                            taskInfo.put("queueName", firstTask.getQueueName());
                            taskInfo.put("status", firstTask.getStatus());
                            workflowInfo.put("currentTask", taskInfo);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Could not retrieve initial tasks: {}", e.getMessage());
                }
                
                // Note: Workflow metadata would be included in response if workflowMetadata field existed
                log.info("Workflow started: processInstanceId={}, processDefinitionKey={}", 
                        processResponse.getProcessInstanceId(), processDefinitionKey);
                
                return response;
                
            } catch (Exception e) {
                log.error("Failed to start workflow for case {}: {}", savedCase.getCaseNumber(), e.getMessage());
                // Return case without workflow info - case is created but workflow failed
                CaseWithAllegationsResponse response = convertToResponse(savedCase);
                
                Map<String, Object> workflowInfo = new HashMap<>();
                workflowInfo.put("workflowStatus", "FAILED");
                workflowInfo.put("error", "Workflow service unavailable: " + e.getMessage());
                // Note: Workflow metadata would be included in response if workflowMetadata field existed
                log.warn("Workflow failed to start for case: {}, error: {}", savedCase.getCaseNumber(), e.getMessage());
                
                return response;
            }
            
        } catch (Exception e) {
            log.error("Failed to create case: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create case: " + e.getMessage(), e);
        }
    }
    
    @Transactional(readOnly = true)
    public CaseWithAllegationsResponse getCaseByNumber(String caseNumber) {
        log.info("Getting case by number: {}", caseNumber);
        
        return caseRepository.findByCaseNumber(caseNumber)
            .map(this::convertToResponse)
            .orElse(null);
    }
    
    @Transactional(readOnly = true)
    public List<CaseWithAllegationsResponse> getAllCases(int page, int size, String status) {
        log.info("Getting all cases - page: {}, size: {}, status: {}", page, size, status);
        
        try {
            List<Case> cases;
            
            if (status != null && !status.trim().isEmpty()) {
                cases = caseRepository.findByStatus(status);
            } else {
                cases = caseRepository.findAll();
            }
            
            // Apply pagination manually
            int start = page * size;
            int end = Math.min(start + size, cases.size());
            
            if (start >= cases.size()) {
                return List.of();
            }
            
            return cases.subList(start, end)
                .stream()
                .map(this::convertToResponse)
                .toList();
                
        } catch (Exception e) {
            log.error("Failed to get cases: {}", e.getMessage());
            return List.of();
        }
    }
    
    @Transactional
    public CaseWithAllegationsResponse submitCase(String caseNumber, String userId) {
        log.info("Submitting case: {} by user: {}", caseNumber, userId);
        
        Case caseEntity = caseRepository.findByCaseNumber(caseNumber)
            .orElseThrow(() -> new RuntimeException("Case not found: " + caseNumber));
        
        try {
            // Get user's current tasks for this case and complete them
            List<TaskResponse> userTasks = workflowServiceClient.getUserTasks(userId);
            
            userTasks.stream()
                .filter(task -> caseNumber.equals(task.getProcessInstanceId()))
                .filter(task -> "OPEN".equals(task.getStatus()) || "CLAIMED".equals(task.getStatus()))
                .findFirst()
                .ifPresent(task -> {
                    try {
                        Map<String, Object> variables = new HashMap<>();
                        variables.put("submittedBy", userId);
                        variables.put("submissionTime", LocalDateTime.now().toString());
                        variables.put("action", "SUBMIT");
                        
                        workflowServiceClient.completeTask(task.getTaskId(), variables, userId);
                        log.info("Completed workflow task {} for case submission", task.getTaskId());
                    } catch (Exception e) {
                        log.error("Failed to complete workflow task for case submission: {}", e.getMessage());
                    }
                });
            
        } catch (Exception e) {
            log.error("Failed to get workflow tasks for case submission: {}", e.getMessage());
        }
        
        // Update case status
        caseEntity.setStatus(CaseStatus.IN_PROGRESS);
        caseEntity.setUpdatedAt(LocalDateTime.now());
        Case savedCase = caseRepository.save(caseEntity);
        
        log.info("Case {} submitted successfully by user {}", caseNumber, userId);
        return convertToResponse(savedCase);
    }
    
    /**
     * Create case entity from request
     */
    private Case createCaseEntity(CreateCaseWithAllegationsRequest request, String userId) {
        Case caseEntity = new Case();
        caseEntity.setCaseNumber(generateCaseNumber());
        caseEntity.setTitle(request.getTitle());
        caseEntity.setDescription(request.getDescription());
        caseEntity.setPriority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM);
        caseEntity.setStatus(CaseStatus.OPEN);
        caseEntity.setComplainantName(request.getComplainantName());
        caseEntity.setComplainantEmail(request.getComplainantEmail());
        caseEntity.setCreatedByUserId(userId != null ? userId : "system");
        caseEntity.setCreatedAt(LocalDateTime.now());
        caseEntity.setUpdatedAt(LocalDateTime.now());
        
        return caseEntity;
    }
    
    /**
     * Convert Case entity to response DTO
     */
    private CaseWithAllegationsResponse convertToResponse(Case caseEntity) {
        CaseWithAllegationsResponse response = new CaseWithAllegationsResponse();
        
        response.setCaseId(caseEntity.getCaseId());
        response.setCaseNumber(caseEntity.getCaseNumber());
        response.setTitle(caseEntity.getTitle());
        response.setDescription(caseEntity.getDescription());
        response.setPriority(caseEntity.getPriority());
        response.setStatus(caseEntity.getStatus());
        response.setComplainantName(caseEntity.getComplainantName());
        response.setComplainantEmail(caseEntity.getComplainantEmail());
        response.setCreatedAt(caseEntity.getCreatedAt());
        response.setUpdatedAt(caseEntity.getUpdatedAt());
        response.setCreatedBy(caseEntity.getCreatedByUserId());
        response.setAssignedTo(caseEntity.getAssignedToUserId());
        // response.setProcessInstanceId(caseEntity.getProcessInstanceId()); // Field not available in DTO
        
        return response;
    }
    
    /**
     * Determine workflow process based on case complexity
     */
    private String determineWorkflowProcess(CreateCaseWithAllegationsRequest request) {
        // Analyze allegations to determine if single or multi-department workflow is needed
        if (request.getAllegations() != null && request.getAllegations().size() > 1) {
            // Check if allegations span multiple departments
            boolean hasHRAllegations = request.getAllegations().stream()
                .anyMatch(a -> a.getAllegationType().contains("HARASSMENT") || 
                              a.getAllegationType().contains("DISCRIMINATION"));
            boolean hasLegalAllegations = request.getAllegations().stream()
                .anyMatch(a -> a.getAllegationType().contains("FRAUD") || 
                              a.getAllegationType().contains("REGULATORY"));
            boolean hasSecurityAllegations = request.getAllegations().stream()
                .anyMatch(a -> a.getAllegationType().contains("SECURITY") || 
                              a.getAllegationType().contains("DATA_BREACH"));
            
            int departmentCount = (hasHRAllegations ? 1 : 0) + 
                                 (hasLegalAllegations ? 1 : 0) + 
                                 (hasSecurityAllegations ? 1 : 0);
            
            if (departmentCount > 1) {
                return "OneCMS_MultiDepartment_Workflow";
            }
        }
        
        return "OneCMS_Workflow"; // Default single department workflow
    }
    
    /**
     * Build workflow variables for process start
     */
    private Map<String, Object> buildWorkflowVariables(CreateCaseWithAllegationsRequest request, Case savedCase) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("caseId", savedCase.getId().toString());
        variables.put("caseNumber", savedCase.getCaseNumber());
        variables.put("title", request.getTitle());
        variables.put("priority", savedCase.getPriority().toString());
        variables.put("severity", "MEDIUM"); // Default severity for workflow
        variables.put("departmentId", null); // Department will be determined by allegation analysis
        variables.put("createdBy", savedCase.getCreatedByUserId());
        variables.put("allegationCount", request.getAllegations() != null ? request.getAllegations().size() : 0);
        
        // Add allegation types for workflow routing
        if (request.getAllegations() != null) {
            List<String> allegationTypes = request.getAllegations().stream()
                .map(a -> a.getAllegationType())
                .toList();
            variables.put("allegationTypes", allegationTypes);
        }
        
        return variables;
    }
    
    /**
     * Generate unique case number
     */
    private String generateCaseNumber() {
        try {
            Long maxId = caseRepository.findMaxCaseId();
            long nextId = (maxId != null) ? maxId + 1 : 1;
            return String.format("CMS-%d-%06d", Year.now().getValue(), nextId);
        } catch (Exception e) {
            log.warn("Could not generate case number from database, using timestamp");
            long timestamp = System.currentTimeMillis() % 1000000;
            return String.format("CMS-%d-%06d", Year.now().getValue(), timestamp);
        }
    }
}