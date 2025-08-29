package com.workflow.entitlements.service.authorization;

import com.workflow.entitlements.dto.request.AuthorizationCheckRequest;
import com.workflow.entitlements.dto.response.AuthorizationCheckResponse;

import java.util.UUID;

/**
 * Core interface for authorization engines in the hybrid authorization system.
 * 
 * This interface abstracts the authorization logic allowing the system to switch
 * between different authorization engines (Database RBAC vs Cerbos ABAC) based
 * on configuration.
 * 
 * Implementations:
 * - DatabaseAuthorizationEngine: Uses database roles and permissions (RBAC)
 * - CerbosAuthorizationEngine: Uses Cerbos policy engine (ABAC)
 */
public interface AuthorizationEngine {
    
    /**
     * Core authorization check method.
     * 
     * @param request Authorization request containing principal, resource, and action
     * @return Authorization response with decision and reasoning
     */
    AuthorizationCheckResponse checkAuthorization(AuthorizationCheckRequest request);
    
    /**
     * Check if a user has permission for a specific resource and action.
     * Convenience method that builds the AuthorizationCheckRequest internally.
     * 
     * @param userId User ID (UUID)
     * @param resourceType Type of resource (e.g., "case", "workflow")
     * @param resourceId Specific resource ID
     * @param action Action being performed (e.g., "read", "update", "delete")
     * @return Authorization response with decision
     */
    AuthorizationCheckResponse checkUserPermission(UUID userId, String resourceType, 
                                                   String resourceId, String action);
    
    /**
     * Build principal context for a user.
     * Retrieves user information, roles, departments, and attributes needed
     * for authorization decisions.
     * 
     * @param userId User ID (UUID)
     * @return Principal with user context, or null if user not found
     */
    AuthorizationCheckRequest.Principal buildPrincipal(UUID userId);
    
    /**
     * Check if the authorization engine is healthy and ready to process requests.
     * 
     * @return true if engine is healthy, false otherwise
     */
    boolean isEngineHealthy();
    
    /**
     * Get the engine type identifier.
     * 
     * @return Engine type ("DATABASE" or "CERBOS")
     */
    String getEngineType();
    
    /**
     * Perform any necessary initialization for the engine.
     * Called during application startup.
     */
    default void initialize() {
        // Default implementation - no initialization needed
    }
    
    /**
     * Perform any necessary cleanup for the engine.
     * Called during application shutdown.
     */
    default void shutdown() {
        // Default implementation - no cleanup needed
    }
}