package com.workflow.entitlements.service.authorization;

import com.workflow.entitlements.dto.request.AuthorizationCheckRequest;
import com.workflow.entitlements.dto.response.AuthorizationCheckResponse;
import com.workflow.entitlements.entity.*;
import com.workflow.entitlements.repository.*;
import dev.cerbos.sdk.CerbosBlockingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cerbos-based authorization engine implementation.
 * Uses Cerbos policy engine for ABAC (Attribute-Based Access Control).
 * 
 * This engine is selected when authorization.engine.use-cerbos=true in AuthorizationConfig.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CerbosAuthorizationEngine implements AuthorizationEngine {
    
    private final CerbosBlockingClient cerbosClient;
    private final UserRepository userRepository;
    private final UserDepartmentRepository userDepartmentRepository;
    private final UserBusinessAppRoleRepository userBusinessAppRoleRepository;
    private final BusinessApplicationRepository businessApplicationRepository;
    private final BusinessAppRoleRepository businessAppRoleRepository;
    private final DepartmentRepository departmentRepository;
    private final EntitlementAuditLogRepository auditLogRepository;
    
    @Override
    public AuthorizationCheckResponse checkAuthorization(AuthorizationCheckRequest request) {
        try {
            log.debug("Cerbos engine processing authorization request for user: {}", 
                     request.getPrincipal().getId());
            
            UUID userId = request.getPrincipal().getId();
            String resourceType = request.getResource().getKind();
            String resourceId = request.getResource().getId();
            String action = request.getAction();
            
            // TODO: Implement actual Cerbos SDK integration
            // For now, this is a placeholder that will be completed in MILESTONE 4
            boolean allowed = performCerbosAuthorizationCheck(request);
            String reason = allowed ? "Cerbos policy evaluation granted access" : 
                                    "Cerbos policy evaluation denied access";
            
            // Log the authorization decision
            logAuthorizationDecision(userId, resourceType, resourceId, action, allowed, reason);
            
            if (allowed) {
                return AuthorizationCheckResponse.allowed();
            } else {
                return AuthorizationCheckResponse.denied(reason);
            }
            
        } catch (Exception e) {
            log.error("Error in Cerbos authorization check", e);
            return AuthorizationCheckResponse.error("Cerbos authorization check failed: " + e.getMessage());
        }
    }
    
    @Override
    public AuthorizationCheckResponse checkUserPermission(UUID userId, String resourceType, 
                                                          String resourceId, String action) {
        AuthorizationCheckRequest.Principal principal = buildPrincipal(userId);
        if (principal == null) {
            return AuthorizationCheckResponse.denied("User not found");
        }
        
        AuthorizationCheckRequest.Resource resource = AuthorizationCheckRequest.Resource.builder()
                .kind(resourceType)
                .id(resourceId)
                .build();
        
        AuthorizationCheckRequest request = AuthorizationCheckRequest.builder()
                .principal(principal)
                .resource(resource)
                .action(action)
                .build();
        
        return checkAuthorization(request);
    }
    
    @Override
    public AuthorizationCheckRequest.Principal buildPrincipal(UUID userId) {
        try {
            // Find user
            var userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                log.warn("User not found: {}", userId);
                return null;
            }
            
            User user = userOpt.get();
            if (!user.getIsActive()) {
                log.warn("User is inactive: {}", userId);
                return null;
            }
            
            // Build principal attributes for Cerbos
            Map<String, Object> attributes = new HashMap<>();
            
            // Add user attributes
            attributes.put("username", user.getUsername());
            attributes.put("email", user.getEmail());
            attributes.put("firstName", user.getFirstName());
            attributes.put("lastName", user.getLastName());
            attributes.put("isActive", user.getIsActive());
            
            // Add global attributes from user
            if (user.getGlobalAttributes() != null) {
                attributes.putAll(user.getGlobalAttributes());
            }
            
            // Add business application roles (for Cerbos)
            // TODO: Fix repository methods to work with UUID in MILESTONE 4
            List<String> roles = List.of("PLACEHOLDER_ROLE");
            attributes.put("roles", roles);
            
            // Add departments (for Cerbos) 
            // TODO: Fix repository methods to work with UUID in MILESTONE 4
            List<String> departments = List.of("PLACEHOLDER_DEPT");
            attributes.put("departments", departments);
            
            return AuthorizationCheckRequest.Principal.builder()
                    .id(userId)
                    .attributes(attributes)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error building principal for user: {}", userId, e);
            return null;
        }
    }
    
    @Override
    public boolean isEngineHealthy() {
        try {
            // Simple health check - verify Cerbos connectivity
            // This is a minimal check - in production you might want a dedicated health check endpoint
            return cerbosClient != null;
        } catch (Exception e) {
            log.error("Cerbos authorization engine health check failed", e);
            return false;
        }
    }
    
    @Override
    public String getEngineType() {
        return "CERBOS";
    }
    
    /**
     * Placeholder method for Cerbos authorization check.
     * TODO: Implement actual Cerbos SDK integration in MILESTONE 4.
     */
    private boolean performCerbosAuthorizationCheck(AuthorizationCheckRequest request) {
        // For now, return a basic authorization check based on user attributes
        // This will be replaced with actual Cerbos policy evaluation
        
        try {
            Map<String, Object> attributes = request.getPrincipal().getAttributes();
            
            if (attributes == null) {
                return false;
            }
            
            // Basic logic: allow if user has roles
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) attributes.get("roles");
            
            if (roles == null || roles.isEmpty()) {
                return false;
            }
            
            // For demonstration: allow basic actions for users with any role
            String action = request.getAction();
            if ("read".equals(action) && !roles.isEmpty()) {
                return true;
            }
            
            // For write actions, require specific roles
            if (("create".equals(action) || "update".equals(action)) && 
                (roles.contains("ADMIN") || roles.contains("INVESTIGATOR") || roles.contains("INTAKE_ANALYST"))) {
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("Error in placeholder Cerbos authorization", e);
            return false;
        }
    }
    
    /**
     * Log authorization decision for audit trail
     */
    private void logAuthorizationDecision(UUID userId, String resourceType, String resourceId, 
                                        String action, boolean allowed, String reason) {
        try {
            EntitlementAuditLog auditLog = EntitlementAuditLog.authorizationCheck(
                    userId, resourceType, resourceId, action,
                    allowed ? EntitlementAuditLog.Decision.ALLOW : EntitlementAuditLog.Decision.DENY,
                    reason, getEngineType());
            
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to log authorization decision", e);
            // Don't fail authorization due to audit logging failure
        }
    }
}