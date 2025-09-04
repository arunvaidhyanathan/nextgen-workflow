package com.citi.onecms.service;

import com.citi.onecms.client.WorkflowServiceClient;
import com.citi.onecms.dto.CaseDraftResponse;
import com.citi.onecms.dto.CreateCaseDraftRequest;
import com.citi.onecms.dto.workflow.StartProcessWithTaskResponse;
import com.citi.onecms.dto.workflow.TaskResponse;
import com.citi.onecms.entity.Case;
import com.citi.onecms.entity.CaseDraft;
import com.citi.onecms.entity.CaseType;
import com.citi.onecms.entity.CaseStatus;
import com.citi.onecms.enums.TaskStatus;
import com.citi.onecms.repository.CaseDraftRepository;
import com.citi.onecms.repository.CaseRepository;
import com.citi.onecms.repository.CaseTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing case drafts with workflow integration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaseDraftService {
    
    private final CaseDraftRepository caseDraftRepository;
    private final CaseRepository caseRepository;
    private final CaseTypeRepository caseTypeRepository;
    private final WorkflowServiceClient workflowServiceClient;
    
    /**
     * Create a new case draft and start the workflow process
     */
    @Transactional
    public CaseDraftResponse createCaseDraft(CreateCaseDraftRequest request, String userId) {
        log.info("Creating case draft: {} by user: {}", request.getCaseDraftTitle(), userId);
        
        try {
            // 1. Create the case entity first (required for foreign key)
            Case caseEntity = createCaseEntity(request, userId);
            Case savedCase = caseRepository.save(caseEntity);
            log.info("Case entity created with ID: {} and case number: {}", savedCase.getId(), savedCase.getCaseNumber());
            
            // 2. Determine process definition key if not provided
            String processDefinitionKey = determineProcessDefinitionKey(request);
            String businessKey = generateBusinessKey(request, savedCase.getCaseNumber());
            
            // 3. Start workflow process with task information
            StartProcessWithTaskResponse workflowResponse = startWorkflowProcess(
                processDefinitionKey, businessKey, request, savedCase, userId);
            
            // 4. Create case draft entity with workflow information
            CaseDraft caseDraft = createCaseDraftEntity(request, savedCase, workflowResponse, userId);
            CaseDraft savedCaseDraft = caseDraftRepository.save(caseDraft);
            
            // 5. Update case with process instance ID
            savedCase.setProcessInstanceId(workflowResponse.getProcessInstanceId());
            caseRepository.save(savedCase);
            
            log.info("Case draft created successfully: ID={}, ProcessInstanceId={}, InitialTaskId={}", 
                    savedCaseDraft.getCaseDraftId(), 
                    workflowResponse.getProcessInstanceId(),
                    workflowResponse.getInitialTask() != null ? workflowResponse.getInitialTask().getTaskId() : null);
            
            // Ensure case entity is loaded for response mapping
            savedCaseDraft.setCaseEntity(savedCase);
            return CaseDraftResponse.from(savedCaseDraft);
            
        } catch (Exception e) {
            log.error("Failed to create case draft for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to create case draft: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get case draft by ID
     */
    public Optional<CaseDraftResponse> getCaseDraftById(Long caseDraftId) {
        return caseDraftRepository.findById(caseDraftId)
                .map(CaseDraftResponse::from);
    }
    
    /**
     * Get case drafts by user
     */
    public List<CaseDraftResponse> getCaseDraftsByUser(String userId) {
        return caseDraftRepository.findByCreatedByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(CaseDraftResponse::from)
                .toList();
    }
    
    /**
     * Get case drafts by user with pagination
     */
    public Page<CaseDraftResponse> getCaseDraftsByUser(String userId, Pageable pageable) {
        return caseDraftRepository.findByCreatedByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(CaseDraftResponse::from);
    }
    
    /**
     * Get case draft by process instance ID
     */
    public Optional<CaseDraftResponse> getCaseDraftByProcessInstanceId(String processInstanceId) {
        return caseDraftRepository.findByProcessInstanceId(processInstanceId)
                .map(CaseDraftResponse::from);
    }
    
    /**
     * Submit case draft (mark as submitted)
     */
    @Transactional
    public CaseDraftResponse submitCaseDraft(Long caseDraftId, String userId) {
        CaseDraft caseDraft = caseDraftRepository.findById(caseDraftId)
                .orElseThrow(() -> new RuntimeException("Case draft not found: " + caseDraftId));
        
        // Verify user can submit this draft
        if (!caseDraft.getCreatedByUserId().equals(userId)) {
            throw new RuntimeException("User not authorized to submit this case draft");
        }
        
        // Update draft status
        caseDraft.setDraftStatus(CaseDraft.DraftStatus.SUBMITTED);
        caseDraft.setIsSubmitted(true);
        caseDraft.setSubmittedAt(LocalDateTime.now());
        
        CaseDraft savedCaseDraft = caseDraftRepository.save(caseDraft);
        log.info("Case draft submitted: ID={} by user: {}", caseDraftId, userId);
        
        return CaseDraftResponse.from(savedCaseDraft);
    }
    
    // Private helper methods
    
    private Case createCaseEntity(CreateCaseDraftRequest request, String userId) {
        Case caseEntity = new Case();
        caseEntity.setCaseNumber(generateCaseNumber());
        caseEntity.setTitle(request.getCaseDraftTitle());
        caseEntity.setDescription(request.getCaseDraftDescription());
        caseEntity.setStatus(CaseStatus.DRAFT); // Start as draft
        caseEntity.setPriority(request.getPriority());
        caseEntity.setCreatedByUserId(userId);
        
        if (request.getCaseTypeId() != null) {
            // Load CaseType entity if provided - for now set to null, will be handled by relationship
            // The case_type_id will be handled by JPA if we set the foreign key field directly
            // For now, we'll implement a simple approach and enhance later if needed
            CaseType caseType = caseTypeRepository.findById(request.getCaseTypeId()).orElse(null);
            caseEntity.setCaseType(caseType);
        }
        
        if (request.getDepartmentId() != null) {
            caseEntity.setDepartmentId(request.getDepartmentId());
        }
        
        // Set additional fields from request
        caseEntity.setDateReceivedByEscalationChannel(request.getDateReceivedByEscalationChannel());
        caseEntity.setComplaintEscalatedBy(request.getComplaintReceivedMethod());
        
        return caseEntity;
    }
    
    private String generateCaseNumber() {
        int currentYear = Year.now().getValue();
        Long nextSequence = caseRepository.getNextCaseSequence();
        return String.format("CMS-%d-%06d", currentYear, nextSequence);
    }
    
    private String determineProcessDefinitionKey(CreateCaseDraftRequest request) {
        if (request.getProcessDefinitionKey() != null && !request.getProcessDefinitionKey().isEmpty()) {
            return request.getProcessDefinitionKey();
        }
        
        // Default to the case workflow for case drafts
        return "oneCmsCaseWorkflow";
    }
    
    private String generateBusinessKey(CreateCaseDraftRequest request, String caseNumber) {
        if (request.getBusinessKey() != null && !request.getBusinessKey().isEmpty()) {
            return request.getBusinessKey();
        }
        
        // Use the case number as business key for consistent end-to-end tracking
        // This ensures the workflow can be tracked from draft through completion
        return caseNumber;
    }
    
    private StartProcessWithTaskResponse startWorkflowProcess(String processDefinitionKey, String businessKey, 
                                                            CreateCaseDraftRequest request, Case caseEntity, String userId) {
        
        // Prepare workflow variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("caseId", caseEntity.getId());
        variables.put("caseNumber", caseEntity.getCaseNumber());
        variables.put("caseTitle", request.getCaseDraftTitle());
        variables.put("priority", request.getPriority().name());
        variables.put("complaintMethod", request.getComplaintReceivedMethod());
        variables.put("createdBy", userId);
        
        if (request.getDepartmentId() != null) {
            variables.put("departmentId", request.getDepartmentId());
        }
        
        if (request.getCaseTypeId() != null) {
            variables.put("caseTypeId", request.getCaseTypeId());
        }
        
        // Start the process with enhanced response
        return workflowServiceClient.startProcessWithTasks(processDefinitionKey, businessKey, variables, userId);
    }
    
    private CaseDraft createCaseDraftEntity(CreateCaseDraftRequest request, Case caseEntity, 
                                          StartProcessWithTaskResponse workflowResponse, String userId) {
        
        CaseDraft.CaseDraftBuilder builder = CaseDraft.builder()
                .caseId(caseEntity.getId())
                .caseDraftTitle(request.getCaseDraftTitle())
                .caseDraftDescription(request.getCaseDraftDescription())
                .priority(request.getPriority())
                .caseTypeId(request.getCaseTypeId())
                .departmentId(request.getDepartmentId())
                .dateReceivedByEscalationChannel(request.getDateReceivedByEscalationChannel())
                .complaintReceivedMethod(request.getComplaintReceivedMethod())
                .processInstanceId(workflowResponse.getProcessInstanceId())
                .processDefinitionKey(workflowResponse.getProcessDefinitionKey())
                .businessKey(workflowResponse.getBusinessKey())
                .createdByUserId(userId)
                .draftStatus(CaseDraft.DraftStatus.DRAFT)
                .taskStatus(TaskStatus.OPEN);
        
        // Set initial task information if available
        if (workflowResponse.getInitialTask() != null) {
            TaskResponse initialTask = workflowResponse.getInitialTask();
            builder.initialTaskId(initialTask.getTaskId())
                   .initialTaskName(initialTask.getTaskName())
                   .initialTaskQueue(initialTask.getQueueName());
            
            // Extract candidate group from task data if available
            if (initialTask.getTaskData() != null && initialTask.getTaskData().get("candidateGroups") != null) {
                builder.candidateGroup(initialTask.getTaskData().get("candidateGroups").toString());
            }
            
            // Set task status based on workflow task status
            if (initialTask.getStatus() != null) {
                try {
                    TaskStatus status = TaskStatus.valueOf(initialTask.getStatus());
                    builder.taskStatus(status);
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown task status from workflow: {}, defaulting to OPEN", initialTask.getStatus());
                    builder.taskStatus(TaskStatus.OPEN);
                }
            }
        }
        
        return builder.build();
    }
}