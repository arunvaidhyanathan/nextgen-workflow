package com.citi.onecms.service;

import com.citi.onecms.client.EntitlementServiceClient;
import com.citi.onecms.dto.AuthorizationCheckResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for handling authorization checks using Cerbos via EntitlementService.
 * Replaces Spring Security annotations with explicit authorization calls.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {

    private final EntitlementServiceClient entitlementServiceClient;
    
    @Value("${cms.authorization.enabled:false}")
    private boolean authorizationEnabled;
    
    /**
     * Check if user is authorized to perform an action on a resource
     */
    public boolean checkAuthorization(String userId, String resourceKind, String resourceId, 
                                    String action, Map<String, Object> resourceAttributes) {
        try {
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("Authorization check failed: No user ID provided");
                return false;
            }
            
            if (!authorizationEnabled) {
                log.debug("Authorization disabled - allowing access for user={}, action={}, resource={}", 
                         userId, action, resourceKind);
                return true;
            }
            
            log.debug("Checking authorization: user={}, resource={}/{}, action={}", 
                     userId, resourceKind, resourceId, action);
            
            AuthorizationCheckResponse response = entitlementServiceClient.checkAuthorization(
                userId, null, resourceKind, resourceId, resourceAttributes, action);
            
            boolean allowed = response.isAllowed();
            
            log.info("Authorization check result: user={}, action={}, resource={}, allowed={}", 
                    userId, action, resourceKind, allowed);
            
            return allowed;
            
        } catch (Exception e) {
            log.error("Authorization check failed for user={}, action={}, resource={}: {}", 
                     userId, action, resourceKind, e.getMessage());
            return false; // Fail secure
        }
    }
    
    /**
     * Check authorization for case operations with workflow context
     */
    public boolean checkCaseAuthorization(String userId, String caseNumber, String action) {
        Map<String, Object> resourceAttributes = new HashMap<>();
        if (caseNumber != null) {
            resourceAttributes.put("caseNumber", caseNumber);
        }
        
        return checkAuthorization(userId, "case", caseNumber != null ? caseNumber : "new", 
                                action, resourceAttributes);
    }
    
    /**
     * Check authorization for task operations
     */
    public boolean checkTaskAuthorization(String userId, String taskId, String action) {
        Map<String, Object> resourceAttributes = new HashMap<>();
        if (taskId != null) {
            resourceAttributes.put("taskId", taskId);
        }
        
        return checkAuthorization(userId, "task", taskId != null ? taskId : "new", 
                                action, resourceAttributes);
    }
    
    /**
     * Check authorization for queue operations
     */
    public boolean checkQueueAuthorization(String userId, String queueName, String action) {
        Map<String, Object> resourceAttributes = new HashMap<>();
        resourceAttributes.put("queueName", queueName);
        
        return checkAuthorization(userId, "queue", queueName, action, resourceAttributes);
    }
    
    /**
     * Check authorization for workflow registration operations
     */
    public boolean checkWorkflowRegistrationAuthorization(String userId, String workflowId, String action) {
        Map<String, Object> resourceAttributes = new HashMap<>();
        resourceAttributes.put("businessAppName", "onecms");
        if (workflowId != null) {
            resourceAttributes.put("workflowId", workflowId);
        }
        
        return checkAuthorization(userId, "workflow-registration", workflowId != null ? workflowId : "new", 
                                action, resourceAttributes);
    }
    
    /**
     * Check authorization for workflow status/journey operations
     */
    public boolean checkWorkflowStatusAuthorization(String userId, String caseNumber, String action) {
        Map<String, Object> resourceAttributes = new HashMap<>();
        resourceAttributes.put("caseNumber", caseNumber);
        resourceAttributes.put("businessAppName", "onecms");
        
        return checkAuthorization(userId, "workflow-status", caseNumber, action, resourceAttributes);
    }
    
    /**
     * Check authorization for case submission (workflow transition operations)
     */
    public boolean checkCaseSubmissionAuthorization(String userId, String caseNumber) {
        Map<String, Object> resourceAttributes = new HashMap<>();
        resourceAttributes.put("caseNumber", caseNumber);
        resourceAttributes.put("transitionType", "submit");
        
        return checkAuthorization(userId, "case", caseNumber, "submit", resourceAttributes);
    }
    
    /**
     * Extract user ID from HTTP request headers
     */
    public String extractUserId(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("No X-User-Id header found in request");
            return null;
        }
        return userId.trim();
    }
    
    /**
     * Check if user ID is present and return 401 response data if not
     */
    public boolean validateUserPresence(String userId) {
        return userId != null && !userId.trim().isEmpty();
    }
}