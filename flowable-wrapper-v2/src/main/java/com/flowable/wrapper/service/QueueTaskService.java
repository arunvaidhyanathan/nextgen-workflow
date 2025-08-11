package com.flowable.wrapper.service;

import com.flowable.wrapper.dto.response.QueueTaskResponse;
import com.flowable.wrapper.entity.QueueTask;
import com.flowable.wrapper.entity.WorkflowMetadata;
import com.flowable.wrapper.enums.TaskStatus;
import com.flowable.wrapper.exception.ResourceNotFoundException;
import com.flowable.wrapper.model.TaskQueueMapping;
import com.flowable.wrapper.repository.QueueTaskRepository;
import com.flowable.wrapper.repository.WorkflowMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class QueueTaskService {
    
    private final WorkflowMetadataRepository workflowMetadataRepository;
    private final QueueTaskRepository queueTaskRepository;
    private final org.flowable.engine.TaskService taskService;
    private final RuntimeService runtimeService;
    
    /**
     * Populate queue tasks for a newly started process instance
     */
    public void populateQueueTasksForProcessInstance(String processInstanceId, String processDefinitionKey) {
        log.info("Populating queue tasks for process instance: {}", processInstanceId);
        
        // Find workflow metadata
        Optional<WorkflowMetadata> metadataOpt = workflowMetadataRepository
                .findByProcessDefinitionKeyAndActiveTrue(processDefinitionKey);
                
        if (metadataOpt.isEmpty()) {
            log.warn("No workflow metadata found for process: {}", processDefinitionKey);
            return;
        }
        
        WorkflowMetadata metadata = metadataOpt.get();
        
        // Get all active tasks for this process instance
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .list();
                
        log.info("Found {} active tasks for process instance {}", tasks.size(), processInstanceId);
        
        // Process each task
        for (Task task : tasks) {
            try {
                populateQueueTask(task, metadata, processDefinitionKey);
            } catch (Exception e) {
                log.error("Failed to populate queue task for task {}: {}", task.getId(), e.getMessage(), e);
                // Continue with other tasks even if one fails
            }
        }
    }
    
    private void populateQueueTask(Task task, WorkflowMetadata metadata, String processDefinitionKey) {
        // Find the queue for this task
        String queueName = findQueueForTask(task, metadata);
        
        if (queueName == null) {
            log.warn("No queue mapping found for task {} in process {}", 
                task.getTaskDefinitionKey(), processDefinitionKey);
            return;
        }
        
        // Insert into queue_tasks table
        insertQueueTask(task, queueName, processDefinitionKey);
    }
    
    private String findQueueForTask(Task task, WorkflowMetadata metadata) {
        // Look for task in the task queue mappings
        if (metadata.getTaskQueueMappings() != null) {
            for (TaskQueueMapping mapping : metadata.getTaskQueueMappings()) {
                if (mapping.getTaskId().equals(task.getTaskDefinitionKey())) {
                    log.debug("Found queue '{}' for task '{}'", mapping.getQueue(), task.getTaskDefinitionKey());
                    return mapping.getQueue();
                }
            }
        }

        // If not found in mappings, return null
        return null;
    }
    
    private void insertQueueTask(Task task, String queueName, String processDefinitionKey) {
        Map<String, Object> taskData = new HashMap<>();
        taskData.put("description", task.getDescription());
        taskData.put("dueDate", task.getDueDate());
        taskData.put("createTime", task.getCreateTime());
        taskData.put("owner", task.getOwner());
        taskData.put("taskDefinitionKey", task.getTaskDefinitionKey());
        taskData.put("formKey", task.getFormKey());
        
        QueueTask queueTask = QueueTask.builder()
                .taskId(task.getId())
                .processInstanceId(task.getProcessInstanceId())
                .processDefinitionKey(processDefinitionKey)
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .taskName(task.getName())
                .queueName(queueName)
                .assignee(task.getAssignee())
                .status(TaskStatus.OPEN)
                .priority(task.getPriority() > 0 ? task.getPriority() : 50)
                .taskData(taskData)
                .build();
                
        try {
            queueTaskRepository.save(queueTask);
            log.info("Successfully inserted task {} into queue '{}' for process instance {}", 
                task.getId(), queueName, task.getProcessInstanceId());
        } catch (Exception e) {
            log.error("Failed to insert queue task: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to insert queue task", e);
        }
    }
    
    /**
     * Get tasks by queue name
     */
    public List<QueueTaskResponse> getTasksByQueue(String queueName, boolean unassignedOnly) {
        List<QueueTask> tasks;
        if (unassignedOnly) {
            tasks = queueTaskRepository.findByQueueNameAndStatusAndAssigneeIsNullOrderByPriorityDescCreatedAtAsc(queueName, TaskStatus.OPEN);
        } else {
            tasks = queueTaskRepository.findByQueueNameAndStatusOrderByPriorityDescCreatedAtAsc(queueName, TaskStatus.OPEN);
        }
        
        return tasks.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get tasks by queue name with pagination
     */
    public Page<QueueTaskResponse> getTasksByQueue(String queueName, boolean unassignedOnly, Pageable pageable) {
        Page<QueueTask> tasks;
        if (unassignedOnly) {
            tasks = queueTaskRepository.findByQueueNameAndStatusAndAssigneeIsNull(queueName, TaskStatus.OPEN, pageable);
        } else {
            tasks = queueTaskRepository.findByQueueNameAndStatus(queueName, TaskStatus.OPEN, pageable);
        }
        
        return tasks.map(this::mapToResponse);
    }
    
    /**
     * Get tasks by assignee
     */
    public List<QueueTaskResponse> getTasksByAssignee(String userId) {
        List<QueueTask> tasks = queueTaskRepository.findByAssigneeAndStatusInOrderByPriorityDescCreatedAtAsc(
                userId, Arrays.asList(TaskStatus.OPEN, TaskStatus.CLAIMED));
        
        return tasks.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get a single queue task by ID
     */
    public QueueTaskResponse getQueueTask(String taskId) {
        QueueTask queueTask = queueTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));
        
        return mapToResponse(queueTask);
    }
    
    /**
     * Claim a task
     */
    public QueueTaskResponse claimTask(String taskId, String userId) {
        QueueTask queueTask = queueTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));
                
        queueTask.setAssignee(userId);
        queueTask.setStatus(TaskStatus.CLAIMED);
        queueTask.setClaimedAt(Instant.now());
        
        queueTaskRepository.save(queueTask);
        
        log.info("Task {} claimed by user {} in queue", taskId, userId);
        
        return mapToResponse(queueTask);
    }
    
    /**
     * Unclaim a task
     */
    public QueueTaskResponse unclaimTask(String taskId) {
        QueueTask queueTask = queueTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));
                
        queueTask.setAssignee(null);
        queueTask.setStatus(TaskStatus.OPEN);
        queueTask.setClaimedAt(null);
        
        queueTaskRepository.save(queueTask);
        
        log.info("Task {} unclaimed in queue", taskId);
        
        return mapToResponse(queueTask);
    }
    
    /**
     * Complete a task
     */
    public void completeTask(String taskId) {
        QueueTask queueTask = queueTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));
                
        queueTask.setStatus(TaskStatus.COMPLETED);
        queueTask.setCompletedAt(Instant.now());
        
        queueTaskRepository.save(queueTask);
        
        log.info("Task {} marked as completed in queue", taskId);
    }
    
    /**
     * Get tasks by process instance ID
     */
    public List<QueueTaskResponse> getTasksByProcessInstance(String processInstanceId) {
        List<QueueTask> tasks = queueTaskRepository.findByProcessInstanceIdAndStatusOrderByCreatedAtAsc(
                processInstanceId, TaskStatus.OPEN);
        
        return tasks.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get next available (unassigned) task from queue
     */
    public QueueTaskResponse getNextTaskFromQueue(String queueName) {
        // Get unassigned tasks ordered by priority (desc) and creation time (asc)
        List<QueueTask> tasks = queueTaskRepository.findByQueueNameAndStatusAndAssigneeIsNullOrderByPriorityDescCreatedAtAsc(
                queueName, TaskStatus.OPEN);
        
        if (tasks.isEmpty()) {
            return null;
        }
        
        // Return the first task (highest priority, oldest)
        return mapToResponse(tasks.get(0));
    }
    
    /**
     * Map QueueTask entity to response DTO
     */
    private QueueTaskResponse mapToResponse(QueueTask queueTask) {
        QueueTaskResponse response = QueueTaskResponse.builder()
                .taskId(queueTask.getTaskId())
                .processInstanceId(queueTask.getProcessInstanceId())
                .processDefinitionKey(queueTask.getProcessDefinitionKey())
                .taskDefinitionKey(queueTask.getTaskDefinitionKey())
                .taskName(queueTask.getTaskName())
                .queueName(queueTask.getQueueName())
                .assignee(queueTask.getAssignee())
                .status(queueTask.getStatus().getValue())
                .priority(queueTask.getPriority())
                .createdAt(queueTask.getCreatedAt())
                .claimedAt(queueTask.getClaimedAt())
                .completedAt(queueTask.getCompletedAt())
                .taskData(queueTask.getTaskData())
                .build();
                
        // Get business key from Flowable API if process instance exists
        if (queueTask.getProcessInstanceId() != null) {
            try {
                ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                        .processInstanceId(queueTask.getProcessInstanceId())
                        .singleResult();
                if (processInstance != null) {
                    response.setBusinessKey(processInstance.getBusinessKey());
                }
            } catch (Exception e) {
                log.debug("Could not fetch business key for process instance: {}", queueTask.getProcessInstanceId());
            }
        }
        
        return response;
    }
}