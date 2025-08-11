package com.flowable.wrapper.service;

import com.flowable.wrapper.dto.request.StartProcessRequest;
import com.flowable.wrapper.dto.response.ProcessInstanceResponse;
import com.flowable.wrapper.entity.WorkflowMetadata;
import com.flowable.wrapper.exception.ResourceNotFoundException;
import com.flowable.wrapper.exception.WorkflowException;
import com.flowable.wrapper.repository.WorkflowMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProcessInstanceService {
    
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final WorkflowMetadataRepository workflowMetadataRepository;
    private final QueueTaskService queueTaskService;
    
    /**
     * Start a new process instance
     */
    public ProcessInstanceResponse startProcess(StartProcessRequest request) throws WorkflowException {
        log.info("Starting process instance for process: {}", request.getProcessDefinitionKey());
        
        // Verify workflow is registered and deployed
        WorkflowMetadata metadata = workflowMetadataRepository
                .findByProcessDefinitionKeyAndActiveTrue(request.getProcessDefinitionKey())
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", request.getProcessDefinitionKey()));
        
        if (!metadata.getDeployed()) {
            throw new WorkflowException("WORKFLOW_NOT_DEPLOYED", 
                "Workflow '" + request.getProcessDefinitionKey() + "' is not deployed");
        }
        
        try {
            // Prepare variables
            Map<String, Object> variables = request.getVariables() != null ? 
                new HashMap<>(request.getVariables()) : new HashMap<>();
            
            // Start the process instance
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey(request.getProcessDefinitionKey())
                    .businessKey(request.getBusinessKey())
                    .variables(variables)
                    .start();
            
            log.info("Process instance started successfully. Instance ID: {}", processInstance.getId());

            // Populate queue tasks for the started process instance
            queueTaskService.populateQueueTasksForProcessInstance(
                processInstance.getId(), 
                request.getProcessDefinitionKey()
            );

            // Build response
            return ProcessInstanceResponse.builder()
                    .processInstanceId(processInstance.getId())
                    .processDefinitionId(processInstance.getProcessDefinitionId())
                    .processDefinitionKey(processInstance.getProcessDefinitionKey())
                    .processDefinitionName(processInstance.getProcessDefinitionName())
                    .businessKey(processInstance.getBusinessKey())
                    .startTime(Instant.now()) // Flowable doesn't expose start time directly
                    .startedBy(processInstance.getStartUserId())
                    .suspended(processInstance.isSuspended())
                    .variables(variables)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to start process instance: {}", e.getMessage(), e);
            throw new WorkflowException("PROCESS_START_FAILED", 
                "Failed to start process instance: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get process instance by ID - checks both active and historic instances
     */
    public ProcessInstanceResponse getProcessInstance(String processInstanceId) {
        log.info("Getting process instance: {}", processInstanceId);
        
        // First try runtime (active processes)
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .includeProcessVariables()
                .singleResult();
                
        if (processInstance != null) {
            log.info("Found active process instance: {}", processInstanceId);
            return ProcessInstanceResponse.builder()
                    .processInstanceId(processInstance.getId())
                    .processDefinitionId(processInstance.getProcessDefinitionId())
                    .processDefinitionKey(processInstance.getProcessDefinitionKey())
                    .processDefinitionName(processInstance.getProcessDefinitionName())
                    .businessKey(processInstance.getBusinessKey())
                    .startTime(Instant.now()) // Flowable doesn't expose start time directly in runtime
                    .startedBy(processInstance.getStartUserId())
                    .suspended(processInstance.isSuspended())
                    .variables(processInstance.getProcessVariables())
                    .active(true)
                    .build();
        }
        
        // Not found in runtime, check history
        HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .includeProcessVariables()
                .singleResult();
                
        if (historicInstance != null) {
            log.info("Found completed process instance in history: {}", processInstanceId);
            return ProcessInstanceResponse.builder()
                    .processInstanceId(historicInstance.getId())
                    .processDefinitionId(historicInstance.getProcessDefinitionId())
                    .processDefinitionKey(historicInstance.getProcessDefinitionKey())
                    .processDefinitionName(historicInstance.getProcessDefinitionName())
                    .businessKey(historicInstance.getBusinessKey())
                    .startTime(historicInstance.getStartTime() != null ? 
                        historicInstance.getStartTime().toInstant() : null)
                    .endTime(historicInstance.getEndTime() != null ? 
                        historicInstance.getEndTime().toInstant() : null)
                    .startedBy(historicInstance.getStartUserId())
                    .suspended(false) // Completed processes are not suspended
                    .variables(historicInstance.getProcessVariables())
                    .active(false)
                    .durationInMillis(historicInstance.getDurationInMillis())
                    .build();
        }
        
        throw new ResourceNotFoundException("Process instance", processInstanceId);
    }
}