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
            Principal principal = buildPrincipal(request.getPrincipal());
            
            // Build Cerbos Resource
            Resource resource = buildResource(request.getResource());
            
            // Perform authorization check
            CheckResult result = cerbosClient.check(principal, resource, request.getAction());
            
            boolean allowed = result.isAllowed(request.getAction());
            
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
    private Principal buildPrincipal(AuthorizationCheckRequest.Principal principalRequest) {
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
        
        // Build principal with "user" as base role (all authenticated users)
        Principal principal = Principal.newInstance(userId).withRoles("user");
        
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
    private Resource buildResource(AuthorizationCheckRequest.Resource resourceRequest) {
        Resource resource = Resource.newInstance(resourceRequest.getKind(), resourceRequest.getId());
        
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