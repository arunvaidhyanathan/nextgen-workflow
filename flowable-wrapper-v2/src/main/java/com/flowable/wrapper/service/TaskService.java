package com.flowable.wrapper.service;

import com.flowable.wrapper.dto.request.CompleteTaskRequest;
import com.flowable.wrapper.dto.response.QueueTaskResponse;
import com.flowable.wrapper.dto.response.TaskCompletionResponse;
import com.flowable.wrapper.dto.response.TaskDetailResponse;
import com.flowable.wrapper.exception.WorkflowException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.FormService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.form.FormProperty;
import org.flowable.engine.form.TaskFormData;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskService {
    
    private final org.flowable.engine.TaskService flowableTaskService;
    private final RuntimeService runtimeService;
    private final FormService formService;
    private final QueueTaskService queueTaskService;
    
    /**
     * Get tasks by queue name
     */
    public List<QueueTaskResponse> getTasksByQueue(String queueName, boolean unassignedOnly) {
        return queueTaskService.getTasksByQueue(queueName, unassignedOnly);
    }
    
    /**
     * Get tasks by assignee
     */
    public List<QueueTaskResponse> getTasksByAssignee(String userId) {
        return queueTaskService.getTasksByAssignee(userId);
    }
    
    /**
     * Claim a task
     */
    public QueueTaskResponse claimTask(String taskId, String userId) throws WorkflowException {
        // Check if task exists in queue_tasks
        QueueTaskResponse queueTask = queueTaskService.getQueueTask(taskId);
        
        if (queueTask.getAssignee() != null) {
            throw new WorkflowException("TASK_ALREADY_ASSIGNED", 
                "Task is already assigned to: " + queueTask.getAssignee());
        }
        
        // Claim in Flowable
        try {
            flowableTaskService.claim(taskId, userId);
        } catch (Exception e) {
            throw new WorkflowException("CLAIM_FAILED", 
                "Failed to claim task: " + e.getMessage(), e);
        }
        
        // Update queue_tasks table
        return queueTaskService.claimTask(taskId, userId);
    }
    
    /**
     * Complete a task
     */
    public TaskCompletionResponse completeTask(String taskId, String userId, CompleteTaskRequest request) throws WorkflowException {
        // Get task from queue_tasks
        QueueTaskResponse queueTask = queueTaskService.getQueueTask(taskId);
        String taskDefinitionKey = queueTask.getTaskDefinitionKey(); // Capture the original task definition key

        // Verify task is assigned
        if (queueTask.getAssignee() == null) {
            throw new WorkflowException("TASK_NOT_ASSIGNED",
                "Task must be claimed before completion");
        }

        // Verify user is authorized (user must be the assignee)
        if (userId != null && !userId.equals(queueTask.getAssignee())) {
            throw new WorkflowException("UNAUTHORIZED",
                "User " + userId + " is not authorized to complete this task. Task is assigned to: " + queueTask.getAssignee());
        }

        String processInstanceId = queueTask.getProcessInstanceId();
        Map<String, Object> variables = request != null && request.getVariables() != null ?
            request.getVariables() : new HashMap<>();

        // Complete in Flowable
        try {
            flowableTaskService.complete(taskId, variables);
        } catch (Exception e) {
            throw new WorkflowException("COMPLETE_FAILED",
                "Failed to complete task: " + e.getMessage(), e);
        }

        // Update queue_tasks table for the completed task
        Instant completedAt = Instant.now();
        queueTaskService.completeTask(taskId);

        log.info("Task {} completed by user {}", taskId, queueTask.getAssignee());

        // Check if process is still active
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (processInstance != null) {
            // Process is still active, populate next tasks in queue
            queueTaskService.populateQueueTasksForProcessInstance(
                processInstanceId,
                queueTask.getProcessDefinitionKey()
            );

            // Get the newly created active tasks
            List<QueueTaskResponse> nextTasks = queueTaskService.getTasksByProcessInstance(processInstanceId);

            // Check for validation failure (loopback)
            for (QueueTaskResponse nextTask : nextTasks) {
                if (nextTask.getTaskDefinitionKey().equals(taskDefinitionKey)) {
                    // Validation failed, the same task has reappeared
                    log.warn("Validation failed for task definition key: {}. Task {} has looped back.", taskDefinitionKey, taskId);

                    Map<String, Object> processVariables = runtimeService.getVariables(processInstanceId);
                    Object validationError = processVariables.get(taskDefinitionKey + "ValidationError");
                    Object attemptCount = processVariables.get(taskDefinitionKey + "AttemptCount");

                    return TaskCompletionResponse.builder()
                            .status("VALIDATION_FAILED")
                            .message("Please correct the errors and resubmit")
                            .validationErrors(validationError)
                            .attemptNumber(attemptCount instanceof Integer ? (Integer) attemptCount : 1)
                            .retryTaskId(nextTask.getTaskId())
                            .processInstanceId(processInstanceId)
                            .completedAt(completedAt)
                            .completedBy(queueTask.getAssignee())
                            .processActive(true)
                            .build();
                }
            }

            // If no validation failure, return standard success response with next task info
            if (!nextTasks.isEmpty()) {
                QueueTaskResponse nextTask = nextTasks.get(0);
                return TaskCompletionResponse.builder()
                        .status("COMPLETED")
                        .message("Task completed successfully")
                        .taskId(taskId)
                        .taskName(queueTask.getTaskName())
                        .processInstanceId(processInstanceId)
                        .completedAt(completedAt)
                        .completedBy(queueTask.getAssignee())
                        .processActive(true)
                        .nextTaskId(nextTask.getTaskId())
                        .nextTaskName(nextTask.getTaskName())
                        .nextTaskQueue(nextTask.getQueueName())
                        .build();
            }
        }

        // Process is complete
        return TaskCompletionResponse.builder()
                .status("COMPLETED")
                .message("Task completed successfully and process has finished")
                .taskId(taskId)
                .taskName(queueTask.getTaskName())
                .processInstanceId(processInstanceId)
                .completedAt(completedAt)
                .completedBy(queueTask.getAssignee())
                .processActive(false)
                .build();
    }
    
    /**
     * Unclaim a task
     */
    public QueueTaskResponse unclaimTask(String taskId) throws WorkflowException {
        // Check if task exists
        QueueTaskResponse queueTask = queueTaskService.getQueueTask(taskId);
        
        // Unclaim in Flowable
        try {
            flowableTaskService.unclaim(taskId);
        } catch (Exception e) {
            throw new WorkflowException("UNCLAIM_FAILED", 
                "Failed to unclaim task: " + e.getMessage(), e);
        }
        
        // Update queue_tasks table
        return queueTaskService.unclaimTask(taskId);
    }
    
    /**
     * Get next available task from queue
     */
    public QueueTaskResponse getNextTaskFromQueue(String queueName) {
        return queueTaskService.getNextTaskFromQueue(queueName);
    }
    
    /**
     * Get detailed task information including form data
     */
    public TaskDetailResponse getTaskDetails(String taskId) throws WorkflowException {
        // Get task from queue_tasks
        QueueTaskResponse queueTask = queueTaskService.getQueueTask(taskId);
        
        // Get the Flowable task
        Task flowableTask = flowableTaskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();
                
        if (flowableTask == null) {
            throw new com.flowable.wrapper.exception.ResourceNotFoundException("Task", taskId);
        }
        
        // Build response
        TaskDetailResponse response = TaskDetailResponse.builder()
                .taskId(queueTask.getTaskId())
                .processInstanceId(queueTask.getProcessInstanceId())
                .processDefinitionKey(queueTask.getProcessDefinitionKey())
                .taskDefinitionKey(queueTask.getTaskDefinitionKey())
                .taskName(queueTask.getTaskName())
                .queueName(queueTask.getQueueName())
                .assignee(queueTask.getAssignee())
                .status(queueTask.getStatus())
                .priority(queueTask.getPriority())
                .createdAt(queueTask.getCreatedAt())
                .claimedAt(queueTask.getClaimedAt())
                .taskData(queueTask.getTaskData())
                .businessKey(queueTask.getBusinessKey())
                .formKey(flowableTask.getFormKey())
                .description(flowableTask.getDescription())
                .build();
                
        // Set due date if available
        if (flowableTask.getDueDate() != null) {
            response.setDueDate(flowableTask.getDueDate().toInstant());
        }
        
        // Get task variables (form data)
        Map<String, Object> taskVariables = flowableTaskService.getVariables(taskId);
        response.setFormData(taskVariables);
        
        // Get form properties from BPMN definition
        try {
            TaskFormData taskFormData = formService.getTaskFormData(taskId);
            if (taskFormData != null) {
                List<FormProperty> formProperties = taskFormData.getFormProperties();
                Map<String, Object> formPropertiesMap = new HashMap<>();
                for (FormProperty property : formProperties) {
                    Map<String, Object> propertyData = new HashMap<>();
                    propertyData.put("id", property.getId());
                    propertyData.put("name", property.getName());
                    propertyData.put("type", property.getType() != null ? property.getType().getName() : "string");
                    propertyData.put("required", property.isRequired());
                    propertyData.put("readable", property.isReadable());
                    propertyData.put("writable", property.isWritable());
                    if (property.getValue() != null) {
                        propertyData.put("value", property.getValue());
                    }
                    formPropertiesMap.put(property.getId(), propertyData);
                }
                response.setFormProperties(formPropertiesMap);
            } else {
                response.setFormProperties(new HashMap<>());
            }
        } catch (Exception e) {
            log.warn("Could not retrieve form properties for task {}: {}", taskId, e.getMessage());
            response.setFormProperties(new HashMap<>());
        }
        
        // Get process variables
        Map<String, Object> processVariables = runtimeService.getVariables(queueTask.getProcessInstanceId());
        response.setProcessVariables(processVariables);
        
        return response;
    }
}