package com.flowable.wrapper.service;

import com.flowable.wrapper.dto.request.DeployWorkflowRequest;
import com.flowable.wrapper.dto.request.RegisterWorkflowMetadataRequest;
import com.flowable.wrapper.dto.response.WorkflowMetadataResponse;
import com.flowable.wrapper.entity.WorkflowMetadata;
import com.flowable.wrapper.exception.ResourceNotFoundException;
import com.flowable.wrapper.exception.WorkflowException;
import com.flowable.wrapper.model.TaskQueueMapping;
import com.flowable.wrapper.repository.WorkflowMetadataRepository;
import com.flowable.wrapper.security.BpmnSecurityValidator;
import com.flowable.wrapper.security.BpmnValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkflowMetadataService {
    
    private final WorkflowMetadataRepository workflowMetadataRepository;
    private final RepositoryService repositoryService;
    private final BpmnSecurityValidator bpmnSecurityValidator;
    
    @Value("${workflow.definitions.path:/app/definitions}")
    private String definitionsPath;
    
    /**
     * Register workflow metadata with candidate group to queue mappings
     * User provides: candidateGroup -> queue mapping
     */
    public WorkflowMetadataResponse registerWorkflowMetadata(RegisterWorkflowMetadataRequest request) throws WorkflowException {
        log.info("Registering workflow metadata for process: {}", request.getProcessDefinitionKey());
        
        // Check if workflow already exists
        if (workflowMetadataRepository.existsByProcessDefinitionKey(request.getProcessDefinitionKey())) {
            throw new WorkflowException("WORKFLOW_ALREADY_EXISTS", 
                "Workflow already exists with process definition key: " + request.getProcessDefinitionKey());
        }
        
        // Validate candidate group mappings
        if (request.getCandidateGroupMappings() == null || request.getCandidateGroupMappings().isEmpty()) {
            throw new WorkflowException("INVALID_MAPPINGS", 
                "At least one candidate group to queue mapping is required");
        }
        
        // Create and save workflow metadata
        WorkflowMetadata metadata = WorkflowMetadata.builder()
                .processDefinitionKey(request.getProcessDefinitionKey())
                .processName(request.getProcessName())
                .description(request.getDescription())
                .businessAppName(request.getBusinessAppName())
                .candidateGroupMappings(request.getCandidateGroupMappings())
                .metadata(request.getMetadata())
                .build();
        
        metadata = workflowMetadataRepository.save(metadata);
        log.info("Workflow metadata registered successfully with id: {}", metadata.getId());
        
        return toResponse(metadata);
    }
    
    /**
     * Deploy BPMN workflow to Flowable engine and build task mappings
     */
    public WorkflowMetadataResponse deployWorkflow(DeployWorkflowRequest request) throws WorkflowException {
        log.info("Deploying workflow for process: {}", request.getProcessDefinitionKey());
        
        // Get workflow metadata
        WorkflowMetadata metadata = workflowMetadataRepository.findByProcessDefinitionKeyAndActiveTrue(request.getProcessDefinitionKey())
                .orElseThrow(() -> new ResourceNotFoundException("Workflow metadata", request.getProcessDefinitionKey()));
        
        try {
            // Security validation of BPMN content
            log.info("Performing security validation for BPMN: {}", request.getProcessDefinitionKey());
            BpmnValidationResult validationResult = bpmnSecurityValidator.validateBpmnXml(
                request.getBpmnXml(), request.getProcessDefinitionKey());
            
            if (!validationResult.isValid()) {
                log.error("BPMN security validation failed for {}: {}", 
                    request.getProcessDefinitionKey(), validationResult.getSummary());
                throw new WorkflowException("SECURITY_VALIDATION_FAILED", 
                    "BPMN security validation failed: " + validationResult.getSummary());
            }
            
            if (validationResult.getWarningCount() > 0) {
                log.warn("BPMN security validation warnings for {}: {}", 
                    request.getProcessDefinitionKey(), validationResult.getSummary());
            }
            
            // Deploy to Flowable
            String deploymentName = request.getDeploymentName() != null ? 
                request.getDeploymentName() : metadata.getProcessName();
                
            Deployment deployment = repositoryService.createDeployment()
                    .name(deploymentName)
                    .addInputStream(request.getProcessDefinitionKey() + ".bpmn20.xml", 
                        new ByteArrayInputStream(request.getBpmnXml().getBytes(StandardCharsets.UTF_8)))
                    .deploy();
            
            log.info("Workflow deployed successfully. Deployment ID: {}", deployment.getId());
            
            // Get the deployed process definition
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .singleResult();
            
            if (processDefinition != null) {
                // Build task queue mappings from deployed process
                List<TaskQueueMapping> taskMappings = buildTaskQueueMappings(
                    processDefinition.getId(), 
                    metadata.getCandidateGroupMappings()
                );
                
                // Update metadata with deployment info
                metadata.setTaskQueueMappings(taskMappings);
                metadata.setDeployed(true);
                metadata.setDeploymentId(deployment.getId());
                workflowMetadataRepository.save(metadata);
                
                log.info("Built {} task queue mappings for process {}", 
                    taskMappings.size(), processDefinition.getKey());
            }
            
            return toResponse(metadata, deployment.getId());
            
        } catch (Exception e) {
            log.error("Failed to deploy workflow: {}", e.getMessage(), e);
            throw new WorkflowException("DEPLOYMENT_FAILED", 
                "Failed to deploy workflow: " + e.getMessage(), e);
        }
    }
    
    /**
     * Build task queue mappings by querying Flowable for task definitions
     */
    private List<TaskQueueMapping> buildTaskQueueMappings(String processDefinitionId, 
                                                          Map<String, String> candidateGroupMappings) {
        List<TaskQueueMapping> mappings = new ArrayList<>();
        
        try {
            // Get BPMN model from Flowable
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
            
            // Find all user tasks
            Collection<FlowElement> flowElements = bpmnModel.getMainProcess().getFlowElements();
            for (FlowElement element : flowElements) {
                if (element instanceof UserTask) {
                    UserTask userTask = (UserTask) element;
                    
                    // Get candidate groups for this task
                    List<String> candidateGroups = userTask.getCandidateGroups();
                    
                    // If candidateGroups is null or empty, create a new list
                    if (candidateGroups == null) {
                        candidateGroups = new ArrayList<>();
                    } else {
                        // Create a new list from the returned list to ensure it's mutable
                        candidateGroups = new ArrayList<>(candidateGroups);
                    }
                    
                    log.debug("Task '{}' has candidate groups: {}", userTask.getId(), candidateGroups);
                    log.info("DEBUG: UserTask '{}' has extensionElements: {}", userTask.getId(), userTask.getExtensionElements());
                    
                    // Determine which queue this task should go to
                    String assignedQueue = determineQueue(candidateGroups, candidateGroupMappings);
                    
                    // Create task mapping
                    TaskQueueMapping mapping = TaskQueueMapping.builder()
                            .taskId(userTask.getId())
                            .taskName(userTask.getName())
                            .candidateGroups(candidateGroups)
                            .queue(assignedQueue)
                            .metadata(TaskQueueMapping.TaskMetadata.builder()
                                    .documentation(userTask.getDocumentation())
                                    .formKey(userTask.getFormKey())
                                    .category(userTask.getCategory())
                                    .build())
                            .build();
                    
                    mappings.add(mapping);
                    
                    log.debug("Mapped task '{}' with groups {} to queue '{}'", 
                        userTask.getId(), candidateGroups, assignedQueue);
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to build task queue mappings: {}", e.getMessage(), e);
        }
        
        return mappings;
    }
    
    /**
     * Determine which queue a task should go to based on its candidate groups
     */
    private String determineQueue(List<String> candidateGroups, Map<String, String> candidateGroupMappings) {
        if (candidateGroups == null || candidateGroups.isEmpty()) {
            return "default"; // No groups = default queue
        }
        
        // Check each candidate group to find a queue mapping
        for (String group : candidateGroups) {
            String queue = candidateGroupMappings.get(group);
            if (queue != null) {
                return queue; // Use first matching queue
            }
        }
        
        // No mapping found for any group
        log.warn("No queue mapping found for candidate groups: {}. Using default queue.", candidateGroups);
        return "default";
    }
    
    /**
     * Deploy BPMN workflow from a file in the mounted definitions directory
     */
    public WorkflowMetadataResponse deployWorkflowFromFile(String processDefinitionKey, String filename) throws WorkflowException {
        log.info("Deploying workflow from file: {} for process: {}", filename, processDefinitionKey);
        
        // Get workflow metadata
        WorkflowMetadata metadata = workflowMetadataRepository.findByProcessDefinitionKeyAndActiveTrue(processDefinitionKey)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow metadata", processDefinitionKey));
        
        try {
            // Read BPMN file from mounted directory
            // TODO remove the hardcoded path
            Path filePath = Paths.get(definitionsPath, filename);
            if (!Files.exists(filePath)) {
                throw new ResourceNotFoundException("BPMN file", filename);
            }
            
            String bpmnXml = Files.readString(filePath, StandardCharsets.UTF_8);
            log.info("Read BPMN file successfully: {}", filename);
            
            // Security validation of BPMN file content
            log.info("Performing security validation for BPMN file: {}", filename);
            BpmnValidationResult validationResult = bpmnSecurityValidator.validateBpmnXml(
                bpmnXml, processDefinitionKey);
            
            if (!validationResult.isValid()) {
                log.error("BPMN file security validation failed for {}: {}", 
                    filename, validationResult.getSummary());
                throw new WorkflowException("SECURITY_VALIDATION_FAILED", 
                    "BPMN file security validation failed: " + validationResult.getSummary());
            }
            
            if (validationResult.getWarningCount() > 0) {
                log.warn("BPMN file security validation warnings for {}: {}", 
                    filename, validationResult.getSummary());
            }
            
            // Create deploy request and use existing deploy method
            DeployWorkflowRequest deployRequest = new DeployWorkflowRequest();
            deployRequest.setProcessDefinitionKey(processDefinitionKey);
            deployRequest.setBpmnXml(bpmnXml);
            deployRequest.setDeploymentName(metadata.getProcessName() + " - " + filename);
            
            return deployWorkflow(deployRequest);
            
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to read or deploy workflow file: {}", e.getMessage(), e);
            throw new WorkflowException("FILE_DEPLOYMENT_FAILED", 
                "Failed to deploy workflow from file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get workflow metadata by process definition key
     */
    @Transactional(readOnly = true)
    public WorkflowMetadataResponse getWorkflowMetadata(String processDefinitionKey) {
        WorkflowMetadata metadata = workflowMetadataRepository.findByProcessDefinitionKey(processDefinitionKey)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow metadata", processDefinitionKey));
        
        return toResponse(metadata);
    }
    
    private WorkflowMetadataResponse toResponse(WorkflowMetadata metadata) {
        return WorkflowMetadataResponse.builder()
                .id(metadata.getId())
                .processDefinitionKey(metadata.getProcessDefinitionKey())
                .processName(metadata.getProcessName())
                .description(metadata.getDescription())
                .version(metadata.getVersion())
                .businessAppName(metadata.getBusinessAppName())
                .candidateGroupMappings(metadata.getCandidateGroupMappings())
                .taskQueueMappings(metadata.getTaskQueueMappings())
                .metadata(metadata.getMetadata())
                .active(metadata.getActive())
                .createdBy(metadata.getCreatedBy())
                .createdAt(metadata.getCreatedAt())
                .updatedAt(metadata.getUpdatedAt())
                .deployed(metadata.getDeployed())
                .deploymentId(metadata.getDeploymentId())
                .build();
    }
    
    private WorkflowMetadataResponse toResponse(WorkflowMetadata metadata, String deploymentId) {
        // This method is called after deployment, so we can update the metadata if needed
        return WorkflowMetadataResponse.builder()
                .id(metadata.getId())
                .processDefinitionKey(metadata.getProcessDefinitionKey())
                .processName(metadata.getProcessName())
                .description(metadata.getDescription())
                .version(metadata.getVersion())
                .businessAppName(metadata.getBusinessAppName())
                .candidateGroupMappings(metadata.getCandidateGroupMappings())
                .taskQueueMappings(metadata.getTaskQueueMappings())
                .metadata(metadata.getMetadata())
                .active(metadata.getActive())
                .createdBy(metadata.getCreatedBy())
                .createdAt(metadata.getCreatedAt())
                .updatedAt(metadata.getUpdatedAt())
                .deployed(true)
                .deploymentId(deploymentId)
                .build();
    }
}