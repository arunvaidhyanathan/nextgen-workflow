package com.workflow.entitlements.service.authorization;

import com.workflow.entitlements.dto.request.AuthorizationCheckRequest;
import com.workflow.entitlements.dto.response.AuthorizationCheckResponse;
import com.workflow.entitlements.entity.*;
import com.workflow.entitlements.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Database-based authorization engine implementation.
 * Uses database roles and permissions for RBAC (Role-Based Access Control).
 * 
 * This engine is selected when authorization.engine.use-cerbos=false in AuthorizationConfig.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseAuthorizationEngine implements AuthorizationEngine {
    
    private final UserRepository userRepository;
    private final EntitlementUserDomainRoleRepository userDomainRoleRepository;
    private final EntitlementPermissionRepository permissionRepository;
    private final ResourcePermissionRepository resourcePermissionRepository;
    private final DepartmentRepository departmentRepository;
    private final UserDepartmentRepository userDepartmentRepository;
    private final EntitlementAuditLogRepository auditLogRepository;
    
    @Override
    public AuthorizationCheckResponse checkAuthorization(AuthorizationCheckRequest request) {
        try {
            log.debug("Database engine processing authorization request for user: {}", 
                     request.getPrincipal().getId());
            
            UUID userId = request.getPrincipal().getId();
            String resourceType = request.getResource().getKind();
            String resourceId = request.getResource().getId();
            String action = request.getAction();
            
            // Check role-based permissions (RBAC)
            boolean hasRolePermission = checkRoleBasedPermission(userId, resourceType, action);
            
            // Check direct resource permissions (ABAC-style)
            boolean hasResourcePermission = checkResourceBasedPermission(userId, resourceType, resourceId, action);
            
            boolean allowed = hasRolePermission || hasResourcePermission;
            String reason = buildDecisionReason(allowed, hasRolePermission, hasResourcePermission);
            
            // Log the authorization decision
            logAuthorizationDecision(userId, resourceType, resourceId, action, allowed, reason);
            
            if (allowed) {
                return AuthorizationCheckResponse.allowed();
            } else {
                return AuthorizationCheckResponse.denied(reason);
            }
            
        } catch (Exception e) {
            log.error("Error in database authorization check", e);
            return AuthorizationCheckResponse.error("Authorization check failed: " + e.getMessage());
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
            
            // Build principal attributes
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
            
            // Add domain roles
            List<EntitlementUserDomainRole> userRoles = userDomainRoleRepository.findValidRolesByUserId(userId, Instant.now());
            attributes.put("domainRoles", userRoles.stream()
                    .map(ur -> Map.of(
                            "roleId", ur.getRoleId().toString(),
                            "domainId", ur.getRoleId().toString(), // Will be resolved via join
                            "assignedAt", ur.getAssignedAt().toString()
                    ))
                    .toList());
            
            // Add departments
            // TODO: Fix repository methods to work with UUID in MILESTONE 4
            attributes.put("departments", List.of("PLACEHOLDER_DEPT"));
            
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
            // Simple health check - verify database connectivity
            userRepository.count();
            return true;
        } catch (Exception e) {
            log.error("Database authorization engine health check failed", e);
            return false;
        }
    }
    
    @Override
    public String getEngineType() {
        return "DATABASE";
    }
    
    /**
     * Check role-based permissions (RBAC)
     */
    private boolean checkRoleBasedPermission(UUID userId, String resourceType, String action) {
        return permissionRepository.hasUserPermission(userId, resourceType, action);
    }
    
    /**
     * Check direct resource permissions (ABAC-style)
     */
    private boolean checkResourceBasedPermission(UUID userId, String resourceType, String resourceId, String action) {
        return resourcePermissionRepository.hasUserResourcePermission(userId, resourceType, resourceId, action, Instant.now());
    }
    
    /**
     * Build decision reasoning message
     */
    private String buildDecisionReason(boolean allowed, boolean hasRolePermission, boolean hasResourcePermission) {
        if (!allowed) {
            return "User does not have required permissions for this resource and action";
        }
        
        if (hasRolePermission && hasResourcePermission) {
            return "User has both role-based and resource-specific permissions";
        } else if (hasRolePermission) {
            return "User has role-based permission";
        } else if (hasResourcePermission) {
            return "User has direct resource permission";
        }
        
        return "Access granted";
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