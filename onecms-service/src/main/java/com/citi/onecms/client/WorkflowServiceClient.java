package com.citi.onecms.client;

import com.citi.onecms.dto.workflow.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for communicating with the Flowable Workflow Service
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowServiceClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${service.clients.flowable-workflow-service.base-url:http://localhost:8082}")
    private String baseUrl;
    
    private static final String BUSINESS_APP = "onecms";
    
    /**
     * Start a new workflow process
     */
    @CircuitBreaker(name = "flowable-workflow-service", fallbackMethod = "startProcessFallback")
    public StartProcessResponse startProcess(String processDefinitionKey, String businessKey, 
                                           Map<String, Object> variables, String initiator) {
        
        log.info("Starting process: key={}, businessKey={}, initiator={}", processDefinitionKey, businessKey, initiator);
        
        try {
            String url = baseUrl + "/api/" + BUSINESS_APP + "/process-instances/start";
            
            StartProcessRequest request = StartProcessRequest.builder()
                .processDefinitionKey(processDefinitionKey)
                .businessKey(businessKey)
                .variables(variables != null ? variables : new HashMap<>())
                .initiatorId(initiator)
                .build();
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-User-Id", initiator);
            
            HttpEntity<StartProcessRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<StartProcessResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, StartProcessResponse.class);
            
            StartProcessResponse processResponse = response.getBody();
            
            log.info("Process started successfully: processInstanceId={}, businessKey={}", 
                    processResponse != null ? processResponse.getProcessInstanceId() : null, businessKey);
            
            return processResponse;
            
        } catch (Exception e) {
            log.error("Failed to start process for businessKey {}: {}", businessKey, e.getMessage());
            throw new RuntimeException("Workflow service call failed", e);
        }
    }
    
    /**
     * Get tasks for a specific user
     */
    @CircuitBreaker(name = "flowable-workflow-service", fallbackMethod = "getUserTasksFallback")
    public List<TaskResponse> getUserTasks(String userId) {
        
        log.debug("Getting tasks for user: {}", userId);
        
        try {
            String url = baseUrl + "/api/" + BUSINESS_APP + "/tasks/my-tasks";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", userId);
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<TaskResponse[]> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, TaskResponse[].class);
            
            TaskResponse[] tasks = response.getBody();
            List<TaskResponse> taskList = tasks != null ? List.of(tasks) : List.of();
            
            log.debug("Retrieved {} tasks for user {}", taskList.size(), userId);
            
            return taskList;
            
        } catch (Exception e) {
            log.error("Failed to get tasks for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to get user tasks", e);
        }
    }
    
    /**
     * Get tasks by queue name
     */
    @CircuitBreaker(name = "flowable-workflow-service", fallbackMethod = "getTasksByQueueFallback")
    public List<TaskResponse> getTasksByQueue(String queueName, boolean unassignedOnly, String userId) {
        
        log.debug("Getting tasks for queue: {} (unassignedOnly: {})", queueName, unassignedOnly);
        
        try {
            String url = baseUrl + "/api/" + BUSINESS_APP + "/tasks/queue/" + queueName + 
                        "?unassignedOnly=" + unassignedOnly;
            
            HttpHeaders headers = new HttpHeaders();
            if (userId != null) {
                headers.set("X-User-Id", userId);
            }
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<TaskResponse[]> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, TaskResponse[].class);
            
            TaskResponse[] tasks = response.getBody();
            List<TaskResponse> taskList = tasks != null ? List.of(tasks) : List.of();
            
            log.debug("Retrieved {} tasks for queue {}", taskList.size(), queueName);
            
            return taskList;
            
        } catch (Exception e) {
            log.error("Failed to get tasks for queue {}: {}", queueName, e.getMessage());
            throw new RuntimeException("Failed to get queue tasks", e);
        }
    }
    
    /**
     * Claim a task
     */
    @CircuitBreaker(name = "flowable-workflow-service", fallbackMethod = "claimTaskFallback")
    public TaskResponse claimTask(String taskId, String userId) {
        
        log.info("Claiming task: {} for user: {}", taskId, userId);
        
        try {
            String url = baseUrl + "/api/" + BUSINESS_APP + "/tasks/" + taskId + "/claim";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-User-Id", userId);
            
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("assignee", userId);
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<TaskResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, TaskResponse.class);
            
            TaskResponse task = response.getBody();
            
            log.info("Task {} claimed successfully by user {}", taskId, userId);
            
            return task;
            
        } catch (Exception e) {
            log.error("Failed to claim task {} for user {}: {}", taskId, userId, e.getMessage());
            throw new RuntimeException("Failed to claim task", e);
        }
    }
    
    /**
     * Complete a task
     */
    @CircuitBreaker(name = "flowable-workflow-service", fallbackMethod = "completeTaskFallback")
    public void completeTask(String taskId, Map<String, Object> variables, String userId) {
        
        log.info("Completing task: {} by user: {}", taskId, userId);
        
        try {
            String url = baseUrl + "/api/" + BUSINESS_APP + "/tasks/" + taskId + "/complete";
            
            CompleteTaskRequest request = CompleteTaskRequest.builder()
                .variables(variables != null ? variables : new HashMap<>())
                .build();
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-User-Id", userId);
            
            HttpEntity<CompleteTaskRequest> entity = new HttpEntity<>(request, headers);
            
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
            
            log.info("Task {} completed successfully by user {}", taskId, userId);
            
        } catch (Exception e) {
            log.error("Failed to complete task {} for user {}: {}", taskId, userId, e.getMessage());
            throw new RuntimeException("Failed to complete task", e);
        }
    }
    
    // Fallback methods
    
    public StartProcessResponse startProcessFallback(String processDefinitionKey, String businessKey, 
                                                   Map<String, Object> variables, String initiator, Exception ex) {
        log.warn("Workflow service unavailable for process start: {}", ex.getMessage());
        
        StartProcessResponse fallbackResponse = new StartProcessResponse();
        fallbackResponse.setProcessInstanceId("fallback-" + System.currentTimeMillis());
        fallbackResponse.setBusinessKey(businessKey);
        fallbackResponse.setProcessDefinitionKey(processDefinitionKey);
        
        return fallbackResponse;
    }
    
    public List<TaskResponse> getUserTasksFallback(String userId, Exception ex) {
        log.warn("Workflow service unavailable for user tasks: {}", ex.getMessage());
        return List.of();
    }
    
    public List<TaskResponse> getTasksByQueueFallback(String queueName, boolean unassignedOnly, String userId, Exception ex) {
        log.warn("Workflow service unavailable for queue tasks: {}", ex.getMessage());
        return List.of();
    }
    
    public TaskResponse claimTaskFallback(String taskId, String userId, Exception ex) {
        log.warn("Workflow service unavailable for task claim: {}", ex.getMessage());
        return null;
    }
    
    public void completeTaskFallback(String taskId, Map<String, Object> variables, String userId, Exception ex) {
        log.warn("Workflow service unavailable for task completion: {}", ex.getMessage());
        // In a real scenario, you might want to queue this for retry
    }
}