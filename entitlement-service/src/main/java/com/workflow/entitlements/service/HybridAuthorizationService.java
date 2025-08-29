package com.workflow.entitlements.service;

import com.workflow.entitlements.dto.request.AuthorizationCheckRequest;
import com.workflow.entitlements.dto.response.AuthorizationCheckResponse;
import com.workflow.entitlements.service.authorization.AuthorizationEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Hybrid Authorization Service that orchestrates between different authorization engines.
 * 
 * This service acts as the main entry point for all authorization decisions in the system.
 * It automatically selects the appropriate authorization engine based on configuration
 * and provides a unified interface for authorization checks.
 * 
 * Engine Selection:
 * - When authorization.engine.use-cerbos=true: Uses CerbosAuthorizationEngine
 * - When authorization.engine.use-cerbos=false: Uses DatabaseAuthorizationEngine
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HybridAuthorizationService {
    
    /**
     * The active authorization engine (injected based on configuration)
     */
    private final AuthorizationEngine authorizationEngine;
    
    @Value("${authorization.engine.use-cerbos:false}")
    private boolean useCerbos;
    
    /**
     * Perform authorization check using the configured engine.
     * 
     * @param request Authorization request containing principal, resource, and action
     * @return Authorization response with decision and reasoning
     */
    public AuthorizationCheckResponse checkAuthorization(AuthorizationCheckRequest request) {
        log.debug("Processing authorization request using {} engine for user: {}", 
                 authorizationEngine.getEngineType(), request.getPrincipal().getId());
        
        try {
            return authorizationEngine.checkAuthorization(request);
        } catch (Exception e) {
            log.error("Authorization check failed with {} engine", authorizationEngine.getEngineType(), e);
            return AuthorizationCheckResponse.error("Authorization system error: " + e.getMessage());
        }
    }
    
    /**
     * Check if a user has permission for a specific resource and action.
     * Convenience method for simple authorization checks.
     * 
     * @param userId User ID (UUID)
     * @param resourceType Type of resource (e.g., "case", "workflow")
     * @param resourceId Specific resource ID
     * @param action Action being performed (e.g., "read", "update", "delete")
     * @return Authorization response with decision
     */
    public AuthorizationCheckResponse checkUserPermission(UUID userId, String resourceType, 
                                                          String resourceId, String action) {
        log.debug("Checking user permission: user={}, resource={}:{}, action={}", 
                 userId, resourceType, resourceId, action);
        
        try {
            return authorizationEngine.checkUserPermission(userId, resourceType, resourceId, action);
        } catch (Exception e) {
            log.error("User permission check failed", e);
            return AuthorizationCheckResponse.error("Permission check error: " + e.getMessage());
        }
    }
    
    /**
     * Build principal context for a user.
     * Retrieves all necessary information for authorization decisions.
     * 
     * @param userId User ID (UUID)
     * @return Principal with user context, or null if user not found
     */
    public AuthorizationCheckRequest.Principal buildUserPrincipal(UUID userId) {
        log.debug("Building principal for user: {}", userId);
        
        try {
            return authorizationEngine.buildPrincipal(userId);
        } catch (Exception e) {
            log.error("Failed to build principal for user: {}", userId, e);
            return null;
        }
    }
    
    /**
     * Get information about the current authorization configuration.
     * 
     * @return Map containing engine type and configuration details
     */
    public AuthorizationInfo getAuthorizationInfo() {
        return AuthorizationInfo.builder()
                .engineType(authorizationEngine.getEngineType())
                .isHealthy(authorizationEngine.isEngineHealthy())
                .useCerbos(useCerbos)
                .description(getEngineDescription())
                .build();
    }
    
    /**
     * Check if the authorization system is healthy and ready to process requests.
     * 
     * @return true if the system is healthy, false otherwise
     */
    public boolean isAuthorizationSystemHealthy() {
        try {
            return authorizationEngine.isEngineHealthy();
        } catch (Exception e) {
            log.error("Health check failed for authorization engine", e);
            return false;
        }
    }
    
    /**
     * Get a description of the current authorization engine.
     */
    private String getEngineDescription() {
        if ("DATABASE".equals(authorizationEngine.getEngineType())) {
            return "Database-based RBAC engine using roles and permissions";
        } else if ("CERBOS".equals(authorizationEngine.getEngineType())) {
            return "Cerbos-based ABAC engine using policy evaluation";
        } else {
            return "Unknown authorization engine";
        }
    }
    
    /**
     * Data class for authorization system information
     */
    @lombok.Data
    @lombok.Builder
    public static class AuthorizationInfo {
        private String engineType;
        private boolean isHealthy;
        private boolean useCerbos;
        private String description;
    }
}