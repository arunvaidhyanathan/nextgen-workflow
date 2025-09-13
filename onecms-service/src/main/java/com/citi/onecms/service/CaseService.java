package com.citi.onecms.service;

import com.citi.onecms.client.WorkflowServiceClient;
import com.citi.onecms.dto.CreateCaseWithAllegationsRequest;
import com.citi.onecms.dto.CaseWithAllegationsResponse;
import com.citi.onecms.dto.EnhancedCreateCaseRequest;
import com.citi.onecms.dto.workflow.StartProcessResponse;
import com.citi.onecms.dto.workflow.TaskResponse;
import com.citi.onecms.dto.workflow.WorkflowStartResult;
import com.citi.onecms.entity.*;
import com.citi.onecms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Complete Case Service with workflow and authorization integration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaseService {
    
    private final CaseRepository caseRepository;
    private final WorkflowServiceClient workflowServiceClient;
    private final AllegationRepository allegationRepository;
    private final CaseEntityRepository caseEntityRepository;
    private final CaseNarrativeRepository caseNarrativeRepository;
    private final CaseTypeRepository caseTypeRepository;
    private final DepartmentRepository departmentRepository;
    
    @Transactional
    public CaseWithAllegationsResponse createCase(CreateCaseWithAllegationsRequest request, String userId) {
        log.info("Creating comprehensive case: {} by user: {}", request.getTitle(), userId);
        
        try {
            // 1. Create and save case entity first to get the ID
            Case caseEntity = createCaseEntity(request, userId);
            Case savedCase = caseRepository.save(caseEntity);
            log.info("Case entity saved with ID: {} and case number: {}", savedCase.getId(), savedCase.getCaseNumber());
            
            // 2. Create and associate allegations with saved case ID
            if (request.getAllegations() != null && !request.getAllegations().isEmpty()) {
                log.info("Creating {} allegations for case ID: {}", request.getAllegations().size(), savedCase.getId());
                List<Allegation> allegations = createAllegations(request.getAllegations(), savedCase);
                savedCase.setAllegations(allegations);
            }
            
            // 3. Create and associate entities (people/organizations)
            if (request.getEntities() != null && !request.getEntities().isEmpty()) {
                log.info("Creating {} entities for case ID: {}", request.getEntities().size(), savedCase.getId());
                List<CaseEntity> entities = createEntities(request.getEntities(), savedCase);
                savedCase.setEntities(entities);
            }
            
            // 4. Create and associate narratives
            if (request.getNarratives() != null && !request.getNarratives().isEmpty()) {
                log.info("Creating {} narratives for case ID: {}", request.getNarratives().size(), savedCase.getId());
                List<CaseNarrative> narratives = createNarratives(request.getNarratives(), savedCase, userId);
                savedCase.setNarratives(narratives);
            }
            
            // 5. Update case with all relationships
            savedCase = caseRepository.save(savedCase);
            log.info("Case created in database: {} with ID: {}, {} allegations, {} entities, {} narratives", 
                     savedCase.getCaseNumber(), savedCase.getId(),
                     savedCase.getAllegations().size(), 
                     savedCase.getEntities().size(),
                     savedCase.getNarratives().size());
            
            // 6. Start workflow and capture both processInstanceId and taskId
            WorkflowStartResult workflowResult = startWorkflowAndCaptureTasks(savedCase, request, userId);
            
            // 7. Update case with complete workflow information
            if (workflowResult.isSuccessful()) {
                savedCase.setProcessInstanceId(workflowResult.getProcessInstanceId());
                // Task IDs are managed by workflow service, not stored in case entity
                // savedCase.setInitialTaskId(workflowResult.getInitialTaskId());
                // savedCase.setCurrentTaskId(workflowResult.getCurrentTaskId());
                caseRepository.save(savedCase);
                
                log.info("Workflow started successfully: processInstanceId={}, taskId={}", 
                        workflowResult.getProcessInstanceId(), 
                        workflowResult.getInitialTaskId());
            } else {
                log.warn("Workflow failed to start for case: {}, but case was created successfully", savedCase.getCaseNumber());
            }
            
            // 8. Return complete response with all related data
            return convertToCompleteResponse(savedCase, workflowResult);
            
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
                return new ArrayList<>();
            }
            
            return cases.subList(start, end)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Failed to get cases: {}", e.getMessage());
            return new ArrayList<>();
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
     * Enhance existing case with allegations, entities, and narratives
     * Used for Option 1 two-phase approach where case is created first as draft
     */
    @Transactional
    public CaseWithAllegationsResponse enhanceExistingCase(EnhancedCreateCaseRequest request, String userId) {
        log.info("🔄 Enhancing existing case: {} by user: {}", request.getCaseId(), userId);
        
        // 1. Find existing case by case number (case_id is actually case_number)
        Case existingCase = caseRepository.findByCaseNumber(request.getCaseId())
            .orElseThrow(() -> new RuntimeException("Case not found: " + request.getCaseId()));
        
        log.info("Found existing case: ID={}, CaseNumber={}", existingCase.getId(), existingCase.getCaseNumber());
        
        try {
            // 2. Add allegations if provided
            if (request.getAllegations() != null && !request.getAllegations().isEmpty()) {
                log.info("Adding {} allegations to case", request.getAllegations().size());
                List<Allegation> newAllegations = createAllegations(request.getAllegations(), existingCase);
                
                // Add to existing allegations or create new list
                if (existingCase.getAllegations() == null) {
                    existingCase.setAllegations(newAllegations);
                } else {
                    existingCase.getAllegations().addAll(newAllegations);
                }
            }
            
            // 3. Add entities if provided
            if (request.getEntities() != null && !request.getEntities().isEmpty()) {
                log.info("Adding {} entities to case", request.getEntities().size());
                List<CaseEntity> newEntities = createEntities(request.getEntities(), existingCase);
                
                // Add to existing entities or create new list
                if (existingCase.getEntities() == null) {
                    existingCase.setEntities(newEntities);
                } else {
                    existingCase.getEntities().addAll(newEntities);
                }
            }
            
            // 4. Add narratives if provided
            if (request.getNarratives() != null && !request.getNarratives().isEmpty()) {
                log.info("Adding {} narratives to case", request.getNarratives().size());
                List<CaseNarrative> newNarratives = createNarratives(request.getNarratives(), existingCase, userId);
                
                // Add to existing narratives or create new list
                if (existingCase.getNarratives() == null) {
                    existingCase.setNarratives(newNarratives);
                } else {
                    existingCase.getNarratives().addAll(newNarratives);
                }
            }
            
            // 5. Update case timestamp
            existingCase.setUpdatedAt(LocalDateTime.now());
            
            // 6. Save enhanced case
            Case savedCase = caseRepository.save(existingCase);
            log.info("Enhanced case saved: {} with {} allegations, {} entities, {} narratives", 
                     savedCase.getCaseNumber(),
                     savedCase.getAllegations() != null ? savedCase.getAllegations().size() : 0,
                     savedCase.getEntities() != null ? savedCase.getEntities().size() : 0,
                     savedCase.getNarratives() != null ? savedCase.getNarratives().size() : 0);
            
            // 7. Progress workflow by completing current tasks
            try {
                progressWorkflowAfterEnhancement(savedCase, userId);
            } catch (Exception e) {
                log.warn("Failed to progress workflow after case enhancement: {}", e.getMessage());
                // Don't fail the whole operation if workflow progression fails
            }
            
            // 8. Return enhanced case response
            return convertToResponse(savedCase);
            
        } catch (Exception e) {
            log.error("Failed to enhance case {}: {}", request.getCaseId(), e.getMessage(), e);
            throw new RuntimeException("Failed to enhance case: " + e.getMessage(), e);
        }
    }
    
    /**
     * Progress workflow after case enhancement by completing current tasks
     */
    private void progressWorkflowAfterEnhancement(Case caseEntity, String userId) {
        log.info("🔄 Progressing workflow for enhanced case: {}", caseEntity.getCaseNumber());
        
        try {
            // Get current tasks for this process instance
            List<TaskResponse> userTasks = workflowServiceClient.getUserTasks(userId);
            
            // Find tasks related to this case (by process instance ID)
            List<TaskResponse> caseTasks = userTasks.stream()
                .filter(task -> caseEntity.getProcessInstanceId() != null && 
                               caseEntity.getProcessInstanceId().equals(task.getProcessInstanceId()))
                .filter(task -> "OPEN".equals(task.getStatus()) || "CLAIMED".equals(task.getStatus()))
                .collect(Collectors.toList());
            
            if (!caseTasks.isEmpty()) {
                TaskResponse taskToComplete = caseTasks.get(0); // Complete the first available task
                
                // Build completion variables
                Map<String, Object> variables = new HashMap<>();
                variables.put("caseAction", "CREATE");
                variables.put("enhancedBy", userId);
                variables.put("enhancementTime", LocalDateTime.now().toString());
                variables.put("allegationCount", caseEntity.getAllegations() != null ? caseEntity.getAllegations().size() : 0);
                variables.put("entityCount", caseEntity.getEntities() != null ? caseEntity.getEntities().size() : 0);
                variables.put("narrativeCount", caseEntity.getNarratives() != null ? caseEntity.getNarratives().size() : 0);
                
                // Complete the task to progress workflow
                workflowServiceClient.completeTask(taskToComplete.getTaskId(), variables, userId);
                
                log.info("✅ Completed workflow task {} to progress case {}", 
                         taskToComplete.getTaskId(), caseEntity.getCaseNumber());
            } else {
                log.info("No open tasks found for process instance: {}", caseEntity.getProcessInstanceId());
            }
            
        } catch (Exception e) {
            log.error("Failed to progress workflow for enhanced case {}: {}", 
                      caseEntity.getCaseNumber(), e.getMessage());
            throw e;
        }
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
        
        // Set case type if provided
        if (request.getCaseTypeId() != null) {
            CaseType caseType = caseTypeRepository.findById(request.getCaseTypeId())
                .orElseThrow(() -> new RuntimeException("Case type not found with ID: " + request.getCaseTypeId()));
            caseEntity.setCaseType(caseType);
        }
        
        // Set department ID directly (department entity is in entitlements schema)
        if (request.getDepartmentId() != null) {
            caseEntity.setDepartmentId(request.getDepartmentId());
        }
        
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
        response.setProcessInstanceId(caseEntity.getProcessInstanceId());
        
        // Convert allegations to DTOs
        if (caseEntity.getAllegations() != null && !caseEntity.getAllegations().isEmpty()) {
            response.setAllegations(caseEntity.getAllegations().stream()
                .map(this::convertAllegationToResponse)
                .collect(Collectors.toList()));
        }
        
        // Convert entities to DTOs  
        if (caseEntity.getEntities() != null && !caseEntity.getEntities().isEmpty()) {
            response.setEntities(caseEntity.getEntities().stream()
                .map(this::convertEntityToResponse)
                .collect(Collectors.toList()));
        }
        
        // Convert narratives to DTOs
        if (caseEntity.getNarratives() != null && !caseEntity.getNarratives().isEmpty()) {
            response.setNarratives(caseEntity.getNarratives().stream()
                .map(this::convertNarrativeToResponse)
                .collect(Collectors.toList()));
        }
        
        // Add workflow metadata if available
        if (caseEntity.getProcessInstanceId() != null) {
            try {
                CaseWithAllegationsResponse.WorkflowMetadata workflowMetadata = 
                    new CaseWithAllegationsResponse.WorkflowMetadata();
                workflowMetadata.setProcessInstanceId(caseEntity.getProcessInstanceId());
                workflowMetadata.setStatus("ACTIVE");
                response.setWorkflowMetadata(workflowMetadata);
            } catch (Exception e) {
                log.warn("Failed to build workflow metadata: {}", e.getMessage());
            }
        }
        
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
                return "eoCaseWorkflow";
            }
        }
        
        return "eoCaseWorkflow"; // Default single department workflow
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
                .collect(Collectors.toList());
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
    
    /**
     * Create allegations from request
     */
    private List<Allegation> createAllegations(List<CreateCaseWithAllegationsRequest.AllegationRequest> allegationRequests, Case caseEntity) {
        return allegationRequests.stream()
            .map(req -> {
                Allegation allegation = new Allegation();
                allegation.setCaseId(caseEntity.getId());
                allegation.setAllegationId(generateAllegationId()); // Generate unique allegation ID
                allegation.setAllegationType(req.getAllegationType());
                allegation.setSeverity(req.getSeverity());
                allegation.setDescription(req.getDescription());
                // subject field removed - doesn't exist in database schema (use subjectEntityId instead)
                // narrative field removed - doesn't exist in database schema
                allegation.setInvestigationFunction(req.getInvestigationFunction());
                
                // GRC Taxonomy mapping
                allegation.setGrcTaxonomy1(req.getGrcTaxonomyLevel1());
                allegation.setGrcTaxonomy2(req.getGrcTaxonomyLevel2());
                allegation.setGrcTaxonomy3(req.getGrcTaxonomyLevel3());
                allegation.setGrcTaxonomy4(req.getGrcTaxonomyLevel4());
                
                allegation.setCreatedAt(LocalDateTime.now());
                allegation.setUpdatedAt(LocalDateTime.now());
                
                return allegation;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Create case entities (people/organizations) from request
     */
    private List<CaseEntity> createEntities(List<CreateCaseWithAllegationsRequest.EntityRequest> entityRequests, Case caseEntity) {
        return entityRequests.stream()
            .map(req -> {
                CaseEntity entity = new CaseEntity();
                entity.setCaseId(caseEntity.getId());
                entity.setEntityId(generateEntityId());
                entity.setEntityType("Person".equalsIgnoreCase(req.getType()) ? 
                                   CaseEntity.EntityType.PERSON : CaseEntity.EntityType.ORGANIZATION);
                entity.setRelationshipType(req.getRelationshipType());
                entity.setInvestigationFunction(req.getInvestigationFunction());
                
                // Person fields
                if (CaseEntity.EntityType.PERSON.equals(entity.getEntityType())) {
                    entity.setSoeid(req.getSoeid());
                    entity.setGeid(req.getGeid());
                    entity.setFirstName(req.getFirstName());
                    entity.setMiddleName(req.getMiddleName());
                    entity.setLastName(req.getLastName());
                    entity.setHireDate(req.getHireDate());
                    entity.setManager(req.getManager());
                    entity.setGoc(req.getGoc());
                    entity.setHrResponsible(req.getHrResponsible());
                    entity.setLegalVehicle(req.getLegalVehicle());
                    entity.setManagedSegment(req.getManagedSegment());
                    entity.setRelationshipToCiti(req.getRelationshipToCiti());
                    
                    // Set entity_name for person (required field)
                    String entityName = buildPersonDisplayName(req.getFirstName(), req.getMiddleName(), req.getLastName());
                    entity.setEntityName(entityName);
                } else {
                    // Organization fields
                    entity.setOrganizationName(req.getOrganizationName());
                    
                    // Set entity_name for organization (required field)
                    entity.setEntityName(req.getOrganizationName() != null ? req.getOrganizationName() : "Unknown Organization");
                }
                
                // Contact information (common to both)
                entity.setEmailAddress(req.getEmailAddress());
                entity.setPhoneNumber(req.getPhoneNumber());
                entity.setAddress(req.getAddress());
                entity.setCity(req.getCity());
                entity.setState(req.getState());
                entity.setZipCode(req.getZip());
                entity.setPreferredContactMethod(req.getPreferredContactMethod());
                entity.setAnonymous(req.getAnonymous());
                
                entity.setCreatedAt(LocalDateTime.now());
                entity.setUpdatedAt(LocalDateTime.now());
                
                return entity;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Create case narratives from request
     */
    private List<CaseNarrative> createNarratives(List<CreateCaseWithAllegationsRequest.NarrativeRequest> narrativeRequests, Case caseEntity, String userId) {
        return narrativeRequests.stream()
            .map(req -> {
                CaseNarrative narrative = new CaseNarrative();
                narrative.setCaseId(caseEntity.getId());
                narrative.setNarrativeId(generateNarrativeId());
                narrative.setNarrativeType(req.getType());
                narrative.setNarrativeTitle(req.getTitle());
                narrative.setNarrativeText(req.getNarrative());
                narrative.setInvestigationFunction(req.getInvestigationFunction());
                narrative.setAuthor(userId != null ? userId : "System"); // Set author from user context
                narrative.setIsRecalled(false);
                
                narrative.setCreatedAt(LocalDateTime.now());
                narrative.setUpdatedAt(LocalDateTime.now());
                
                return narrative;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Start workflow and capture complete task information
     */
    private WorkflowStartResult startWorkflowAndCaptureTasks(Case savedCase, CreateCaseWithAllegationsRequest request, String userId) {
        try {
            String processDefinitionKey = determineWorkflowProcess(request);
            Map<String, Object> workflowVariables = buildWorkflowVariables(request, savedCase);
            
            // Start the workflow process
            StartProcessResponse processResponse = workflowServiceClient.startProcess(
                processDefinitionKey,
                savedCase.getCaseNumber(),
                workflowVariables,
                userId
            );
            
            // Try to get initial tasks for this process instance
            List<TaskResponse> initialTasks = null;
            String initialTaskId = null;
            String currentTaskId = null;
            
            try {
                // Get all tasks for the user to find ones related to this process instance
                List<TaskResponse> userTasks = workflowServiceClient.getUserTasks(userId);
                initialTasks = userTasks.stream()
                    .filter(task -> processResponse.getProcessInstanceId().equals(task.getProcessInstanceId()))
                    .collect(Collectors.toList());
                
                if (!initialTasks.isEmpty()) {
                    TaskResponse firstTask = initialTasks.get(0);
                    initialTaskId = firstTask.getTaskId();
                    currentTaskId = firstTask.getTaskId(); // Initially same as first task
                }
                
            } catch (Exception e) {
                log.warn("Could not retrieve initial tasks for process instance {}: {}", 
                         processResponse.getProcessInstanceId(), e.getMessage());
            }
            
            return WorkflowStartResult.builder()
                .processInstanceId(processResponse.getProcessInstanceId())
                .processDefinitionKey(processDefinitionKey)
                .businessKey(savedCase.getCaseNumber())
                .initialTaskId(initialTaskId)
                .currentTaskId(currentTaskId)
                .status("STARTED")
                .allInitialTasks(initialTasks != null ? initialTasks : new ArrayList<>())
                .build();
                
        } catch (Exception e) {
            log.error("Failed to start workflow for case {}: {}", savedCase.getCaseNumber(), e.getMessage());
            
            return WorkflowStartResult.builder()
                .processInstanceId(null)
                .processDefinitionKey("OneCMS_Workflow")
                .businessKey(savedCase.getCaseNumber())
                .status("FAILED")
                .allInitialTasks(new ArrayList<>())
                .build();
        }
    }
    
    /**
     * Convert case to complete response with workflow information
     */
    private CaseWithAllegationsResponse convertToCompleteResponse(Case savedCase, WorkflowStartResult workflowResult) {
        CaseWithAllegationsResponse response = convertToResponse(savedCase);
        
        // Add workflow information
        if (workflowResult != null) {
            response.setProcessInstanceId(workflowResult.getProcessInstanceId());
            response.setInitialTaskId(workflowResult.getInitialTaskId());
            response.setCurrentTaskId(workflowResult.getCurrentTaskId());
            
            // Build workflow metadata
            CaseWithAllegationsResponse.WorkflowMetadata workflowMetadata = 
                new CaseWithAllegationsResponse.WorkflowMetadata();
            workflowMetadata.setProcessDefinitionKey(workflowResult.getProcessDefinitionKey());
            workflowMetadata.setProcessInstanceId(workflowResult.getProcessInstanceId());
            workflowMetadata.setStatus(workflowResult.getStatus());
            
            // Add initial task info if available
            if (workflowResult.hasInitialTasks()) {
                TaskResponse firstTask = workflowResult.getFirstTask();
                CaseWithAllegationsResponse.WorkflowMetadata.TaskInfo initialTaskInfo = 
                    new CaseWithAllegationsResponse.WorkflowMetadata.TaskInfo();
                initialTaskInfo.setTaskId(firstTask.getTaskId());
                initialTaskInfo.setTaskName(firstTask.getTaskName());
                initialTaskInfo.setQueueName(firstTask.getQueueName());
                initialTaskInfo.setStatus(firstTask.getStatus());
                workflowMetadata.setInitialTask(initialTaskInfo);
                workflowMetadata.setCurrentTask(initialTaskInfo); // Initially same as initial task
            }
            
            response.setWorkflowMetadata(workflowMetadata);
        }
        
        // Add allegations if they exist
        if (savedCase.getAllegations() != null && !savedCase.getAllegations().isEmpty()) {
            List<CaseWithAllegationsResponse.AllegationResponse> allegationResponses = 
                savedCase.getAllegations().stream()
                    .map(this::convertAllegationToResponse)
                    .collect(Collectors.toList());
            response.setAllegations(allegationResponses);
        }
        
        // Add entities if they exist  
        if (savedCase.getEntities() != null && !savedCase.getEntities().isEmpty()) {
            List<CaseWithAllegationsResponse.EntityResponse> entityResponses = 
                savedCase.getEntities().stream()
                    .map(this::convertEntityToResponse)
                    .collect(Collectors.toList());
            response.setEntities(entityResponses);
        }
        
        // Add narratives if they exist
        if (savedCase.getNarratives() != null && !savedCase.getNarratives().isEmpty()) {
            List<CaseWithAllegationsResponse.NarrativeResponse> narrativeResponses = 
                savedCase.getNarratives().stream()
                    .map(this::convertNarrativeToResponse)
                    .collect(Collectors.toList());
            response.setNarratives(narrativeResponses);
        }
        
        return response;
    }
    
    /**
     * Convert Allegation entity to AllegationResponse
     */
    private CaseWithAllegationsResponse.AllegationResponse convertAllegationToResponse(Allegation allegation) {
        CaseWithAllegationsResponse.AllegationResponse response = new CaseWithAllegationsResponse.AllegationResponse();
        response.setAllegationId(allegation.getId().toString());
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
    
    /**
     * Convert CaseEntity entity to EntityResponse
     */
    private CaseWithAllegationsResponse.EntityResponse convertEntityToResponse(CaseEntity entity) {
        CaseWithAllegationsResponse.EntityResponse response = new CaseWithAllegationsResponse.EntityResponse();
        response.setEntityId(entity.getEntityId());
        response.setEntityType(entity.getEntityType().toString());
        response.setRelationshipType(entity.getRelationshipType());
        response.setDisplayName(entity.getDisplayName()); // Uses utility method in entity
        response.setSoeid(entity.getSoeid());
        response.setEmailAddress(entity.getEmailAddress());
        response.setOrganizationName(entity.getOrganizationName());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
    
    /**
     * Convert CaseNarrative entity to NarrativeResponse
     */
    private CaseWithAllegationsResponse.NarrativeResponse convertNarrativeToResponse(CaseNarrative narrative) {
        CaseWithAllegationsResponse.NarrativeResponse response = new CaseWithAllegationsResponse.NarrativeResponse();
        response.setNarrativeId(narrative.getNarrativeId());
        response.setNarrativeType(narrative.getNarrativeType());
        response.setNarrativeTitle(narrative.getNarrativeTitle());
        response.setNarrativeText(narrative.getNarrativeText());
        response.setInvestigationFunction(narrative.getInvestigationFunction());
        response.setIsRecalled(narrative.getIsRecalled());
        response.setCreatedAt(narrative.getCreatedAt());
        return response;
    }
    
    /**
     * Generate unique entity ID
     */
    private String generateEntityId() {
        return "ENT-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
    
    /**
     * Generate unique narrative ID
     */
    private String generateNarrativeId() {
        return "NAR-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
    
    /**
     * Generate unique allegation ID
     */
    private String generateAllegationId() {
        return "ALG-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
    
    /**
     * Build display name for person entities
     */
    private String buildPersonDisplayName(String firstName, String middleName, String lastName) {
        StringBuilder name = new StringBuilder();
        
        if (firstName != null && !firstName.trim().isEmpty()) {
            name.append(firstName.trim());
        }
        
        if (middleName != null && !middleName.trim().isEmpty()) {
            if (name.length() > 0) name.append(" ");
            name.append(middleName.trim());
        }
        
        if (lastName != null && !lastName.trim().isEmpty()) {
            if (name.length() > 0) name.append(" ");
            name.append(lastName.trim());
        }
        
        // Return a meaningful name or fallback
        return name.length() > 0 ? name.toString() : "Unknown Person";
    }
}