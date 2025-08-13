package com.workflow.entitlements.cerbos;

import com.workflow.entitlements.dto.request.AuthorizationCheckRequest;
import com.workflow.entitlements.dto.response.AuthorizationCheckResponse;
import com.workflow.entitlements.entity.UserBusinessAppRole;
import com.workflow.entitlements.repository.UserBusinessAppRoleRepository;
import com.workflow.entitlements.repository.UserDepartmentRepository;
import dev.cerbos.sdk.CerbosBlockingClient;
import dev.cerbos.sdk.CheckResult;
import dev.cerbos.sdk.CerbosException;
import dev.cerbos.sdk.builders.AttributeValue;
import dev.cerbos.sdk.builders.Principal;
import dev.cerbos.sdk.builders.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CerbosAuthorizationService {
    
    private final CerbosBlockingClient cerbosClient;
    private final UserBusinessAppRoleRepository userBusinessAppRoleRepository;
    private final UserDepartmentRepository userDepartmentRepository;
    
    /**
     * Perform authorization check using Cerbos
     */
    public AuthorizationCheckResponse check(AuthorizationCheckRequest request) {
        try {
            log.debug("Processing authorization check: principal={}, resource={}, action={}", 
                    request.getPrincipal().getId(), request.getResource().getKind(), request.getAction());
            
            // Build Cerbos Principal with user roles
            dev.cerbos.sdk.builders.Principal principal = buildPrincipal(request.getPrincipal());
            
            // Build Cerbos Resource
            dev.cerbos.sdk.builders.Resource resource = buildResource(request.getResource());
            
            // Log request details before Cerbos call
            log.debug("Processing authorization check: principal={}, resource={}, action={}", 
                    request.getPrincipal().getId(), request.getResource().getKind(), request.getAction());
            
            // TEMPORARY BYPASS: Database-based authorization while Cerbos policy loading is fixed
            AuthorizationCheckResponse bypassResult = checkAuthorizationBypass(request);
            if (bypassResult != null) {
                log.info("BYPASS: Authorization decision made via database lookup: principal={}, resource={}, action={}, allowed={}", 
                        request.getPrincipal().getId(), request.getResource().getKind(), request.getAction(), bypassResult.isAllowed());
                return bypassResult;
            }
            
            // Log that we're calling Cerbos (detailed objects logged at DEBUG level in buildPrincipal/buildResource)
            log.debug("Calling Cerbos for authorization check with action: {}", request.getAction());
            
            // Perform authorization check
            CheckResult result = cerbosClient.check(principal, resource, request.getAction());
            
            boolean allowed = result.isAllowed(request.getAction());
            
            // Log detailed result information
            log.debug("Cerbos result details - Allowed: {}, Validation errors: {}", 
                    allowed, 
                    result.getValidationErrors());
            
            log.info("Authorization check result: principal={}, resource={}, action={}, allowed={}", 
                    request.getPrincipal().getId(), request.getResource().getKind(), request.getAction(), allowed);
            
            return allowed ? 
                    AuthorizationCheckResponse.allowed() : 
                    AuthorizationCheckResponse.denied("Policy evaluation denied access");
                    
        } catch (CerbosException e) {
            log.error("Cerbos authorization check failed: principal={}, resource={}, action={}, error={}", 
                    request.getPrincipal().getId(), request.getResource().getKind(), request.getAction(), e.getMessage(), e);
            
            return AuthorizationCheckResponse.error("Cerbos policy evaluation failed: " + e.getMessage());
            
        } catch (Exception e) {
            log.error("Authorization check failed: principal={}, resource={}, action={}, error={}", 
                    request.getPrincipal().getId(), request.getResource().getKind(), request.getAction(), e.getMessage(), e);
            
            return AuthorizationCheckResponse.error("Authorization check failed: " + e.getMessage());
        }
    }
    
    
    /**
     * Build Cerbos Principal with user roles, departments, queues, and other attributes
     */
    private dev.cerbos.sdk.builders.Principal buildPrincipal(AuthorizationCheckRequest.Principal principalRequest) {
        String userId = principalRequest.getId();
        
        // Get user's roles across all business applications
        List<UserBusinessAppRole> userRoles = userBusinessAppRoleRepository.findAllActiveUserRoles(userId);
        
        // Extract role names
        List<String> roles = userRoles.stream()
                .map(uar -> uar.getBusinessAppRole().getRoleName())
                .distinct()
                .collect(Collectors.toList());
        
        // Extract business applications
        List<String> businessApps = userRoles.stream()
                .map(uar -> uar.getBusinessAppRole().getBusinessApplication().getBusinessAppName())
                .distinct()
                .collect(Collectors.toList());
        
        // Get user's departments from user_departments table
        List<String> departments = userDepartmentRepository.findUserDepartmentCodes(userId);
        
        // Extract queues from role metadata
        List<String> queues = extractQueuesFromUserRoles(userRoles);
        
        // Build principal with "user" as base role (all authenticated users) - RESTORED
        dev.cerbos.sdk.builders.Principal principal = dev.cerbos.sdk.builders.Principal.newInstance(userId).withRoles("user");
        
        // Add specific roles
        for (String role : roles) {
            principal = principal.withRoles(role);
        }
        
        // Add business apps as attribute
        if (!businessApps.isEmpty()) {
            List<AttributeValue> businessAppValues = businessApps.stream()
                    .map(AttributeValue::stringValue)
                    .collect(Collectors.toList());
            principal = principal.withAttribute("businessApps", AttributeValue.listValue(businessAppValues));
        }
        
        // Add departments as attribute (critical for policy evaluation)
        if (!departments.isEmpty()) {
            List<AttributeValue> departmentValues = departments.stream()
                    .map(AttributeValue::stringValue)
                    .collect(Collectors.toList());
            principal = principal.withAttribute("departments", AttributeValue.listValue(departmentValues));
        }
        
        // Add queues as attribute (critical for workflow policy evaluation)
        if (!queues.isEmpty()) {
            List<AttributeValue> queueValues = queues.stream()
                    .map(AttributeValue::stringValue)
                    .collect(Collectors.toList());
            principal = principal.withAttribute("queues", AttributeValue.listValue(queueValues));
        }
        
        // Add custom attributes from request
        if (principalRequest.getAttributes() != null) {
            for (Map.Entry<String, Object> entry : principalRequest.getAttributes().entrySet()) {
                AttributeValue attrValue = convertToAttributeValue(entry.getValue());
                if (attrValue != null) {
                    principal = principal.withAttribute(entry.getKey(), attrValue);
                }
            }
        }
        
        log.debug("Built principal: userId={}, roles={}, departments={}, queues={}, businessApps={}", 
                userId, roles, departments, queues, businessApps);
        
        return principal;
    }
    
    /**
     * Extract queues from user business app roles metadata
     */
    private List<String> extractQueuesFromUserRoles(List<UserBusinessAppRole> userRoles) {
        Set<String> allQueues = new HashSet<>();
        
        for (UserBusinessAppRole userRole : userRoles) {
            try {
                // Get metadata from business app role (already parsed as Map)
                Map<String, Object> metadataMap = userRole.getBusinessAppRole().getMetadata();
                if (metadataMap != null && !metadataMap.isEmpty()) {
                    
                    // Extract queues array
                    Object queuesObj = metadataMap.get("queues");
                    if (queuesObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> queues = (List<String>) queuesObj;
                        allQueues.addAll(queues);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to extract queues from metadata for role {}: {}", 
                        userRole.getBusinessAppRole().getRoleName(), e.getMessage());
            }
        }
        
        return new ArrayList<>(allQueues);
    }
    
    /**
     * Build Cerbos Resource
     */
    private dev.cerbos.sdk.builders.Resource buildResource(AuthorizationCheckRequest.Resource resourceRequest) {
        dev.cerbos.sdk.builders.Resource resource = dev.cerbos.sdk.builders.Resource.newInstance(resourceRequest.getKind(), resourceRequest.getId());
        
        // Add resource attributes
        if (resourceRequest.getAttributes() != null) {
            for (Map.Entry<String, Object> entry : resourceRequest.getAttributes().entrySet()) {
                AttributeValue attrValue = convertToAttributeValue(entry.getValue());
                if (attrValue != null) {
                    resource = resource.withAttribute(entry.getKey(), attrValue);
                }
            }
        }
        
        return resource;
    }
    
    /**
     * TEMPORARY BYPASS: Database-based authorization while Cerbos policy loading is fixed
     * Maps all roles and permissions based on the case.yaml policy rules
     */
    private AuthorizationCheckResponse checkAuthorizationBypass(AuthorizationCheckRequest request) {
        try {
            String userId = request.getPrincipal().getId();
            String resourceKind = request.getResource().getKind();
            String action = request.getAction();
            
            // Get user's roles from database
            List<UserBusinessAppRole> userRoles = userBusinessAppRoleRepository.findAllActiveUserRoles(userId);
            Set<String> userRoleNames = userRoles.stream()
                    .map(uar -> uar.getBusinessAppRole().getRoleName())
                    .collect(Collectors.toSet());
            
            log.debug("BYPASS: Checking authorization for user={}, roles={}, resource={}, action={}", 
                    userId, userRoleNames, resourceKind, action);
            
            // Only handle 'case' resource for now
            if (!"case".equals(resourceKind)) {
                return null; // Fall back to Cerbos for other resources
            }
            
            boolean allowed = checkCasePermissions(userRoleNames, action, request);
            
            return allowed ? 
                    AuthorizationCheckResponse.allowed() : 
                    AuthorizationCheckResponse.denied("Database policy evaluation denied access");
                    
        } catch (Exception e) {
            log.error("BYPASS: Error in database authorization check", e);
            return null; // Fall back to Cerbos on error
        }
    }
    
    /**
     * Check case permissions based on database roles (mapped from case.yaml policy)
     */
    private boolean checkCasePermissions(Set<String> userRoles, String action, AuthorizationCheckRequest request) {
        switch (action) {
            case "create":
                // From case.yaml lines 17-19: user, INTAKE_ANALYST
                return userRoles.contains("INTAKE_ANALYST") || hasAnyRole(userRoles);
                
            case "read":
            case "view":
                // From case.yaml lines 26-35: All analyst roles
                return userRoles.contains("INTAKE_ANALYST") ||
                       userRoles.contains("INVESTIGATOR") ||
                       userRoles.contains("INVESTIGATION_MANAGER") ||
                       userRoles.contains("ADJUDICATOR") ||
                       userRoles.contains("AROG_REVIEWER") ||
                       userRoles.contains("EO_OFFICER") ||
                       userRoles.contains("HR_SPECIALIST") ||
                       userRoles.contains("LEGAL_COUNSEL") ||
                       userRoles.contains("SECURITY_ANALYST");
                       
            case "update":
            case "edit":
                // From case.yaml lines 45-47: INVESTIGATION_MANAGER, EO_OFFICER
                return userRoles.contains("INVESTIGATION_MANAGER") ||
                       userRoles.contains("EO_OFFICER");
                       
            case "add_allegation":
                // From case.yaml lines 57-60: INTAKE_ANALYST, INVESTIGATION_MANAGER, INVESTIGATOR
                return userRoles.contains("INTAKE_ANALYST") ||
                       userRoles.contains("INVESTIGATION_MANAGER") ||
                       userRoles.contains("INVESTIGATOR");
                       
            case "update_allegation":
                // From case.yaml lines 66-68: INVESTIGATION_MANAGER, INVESTIGATOR
                return userRoles.contains("INVESTIGATION_MANAGER") ||
                       userRoles.contains("INVESTIGATOR");
                       
            case "add_narrative":
            case "create_narrative":
                // From case.yaml lines 77-82: INVESTIGATOR, INVESTIGATION_MANAGER, HR_SPECIALIST, LEGAL_COUNSEL, SECURITY_ANALYST
                return userRoles.contains("INVESTIGATOR") ||
                       userRoles.contains("INVESTIGATION_MANAGER") ||
                       userRoles.contains("HR_SPECIALIST") ||
                       userRoles.contains("LEGAL_COUNSEL") ||
                       userRoles.contains("SECURITY_ANALYST");
                       
            case "assign":
            case "reassign":
                // From case.yaml lines 96-98: INVESTIGATION_MANAGER, EO_OFFICER
                return userRoles.contains("INVESTIGATION_MANAGER") ||
                       userRoles.contains("EO_OFFICER");
                       
            case "close_case":
            case "reopen_case":
                // From case.yaml lines 105-107: INVESTIGATION_MANAGER, EO_OFFICER
                return userRoles.contains("INVESTIGATION_MANAGER") ||
                       userRoles.contains("EO_OFFICER");
                       
            case "delete":
                // From case.yaml lines 114-115: EO_OFFICER only (with conditions)
                return userRoles.contains("EO_OFFICER");
                
            case "audit":
            case "export":
            case "report":
                // From case.yaml lines 126-129: INVESTIGATION_MANAGER, EO_OFFICER, AROG_REVIEWER
                return userRoles.contains("INVESTIGATION_MANAGER") ||
                       userRoles.contains("EO_OFFICER") ||
                       userRoles.contains("AROG_REVIEWER");
                       
            default:
                log.debug("BYPASS: Unknown action '{}', denying access", action);
                return false;
        }
    }
    
    /**
     * Check if user has any valid role (authenticated user check)
     */
    private boolean hasAnyRole(Set<String> userRoles) {
        return !userRoles.isEmpty();
    }

    /**
     * Convert Java object to Cerbos AttributeValue
     */
    private AttributeValue convertToAttributeValue(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof String) {
            return AttributeValue.stringValue((String) value);
        } else if (value instanceof Boolean) {
            return AttributeValue.boolValue((Boolean) value);
        } else if (value instanceof Number) {
            return AttributeValue.doubleValue(((Number) value).doubleValue());
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<?> list = (List<?>) value;
            List<AttributeValue> attrValues = list.stream()
                    .map(this::convertToAttributeValue)
                    .filter(av -> av != null)
                    .collect(Collectors.toList());
            return AttributeValue.listValue(attrValues);
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            Map<String, AttributeValue> attrMap = map.entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> convertToAttributeValue(entry.getValue())
                    ));
            return AttributeValue.mapValue(attrMap);
        } else {
            // For other types, convert to string
            return AttributeValue.stringValue(value.toString());
        }
    }
}