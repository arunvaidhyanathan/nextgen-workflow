package com.workflow.entitlements.service;

import com.workflow.entitlements.controller.AuthController;
import com.workflow.entitlements.dto.ems.EMSAuthRequest;
import com.workflow.entitlements.dto.ems.EMSAuthResponse;
import com.workflow.entitlements.dto.ems.WhoAmIResponse;
import com.workflow.entitlements.dto.request.AuthorizationCheckRequest;
import com.workflow.entitlements.dto.response.AuthorizationCheckResponse;
import com.workflow.entitlements.entity.BusinessAppRole;
import com.workflow.entitlements.entity.Department;
import com.workflow.entitlements.entity.User;
import com.workflow.entitlements.entity.UserBusinessAppRole;
import com.workflow.entitlements.entity.UserDepartment;
import com.workflow.entitlements.repository.UserDepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * EMS Service for aggregating user context and handling authorization checks.
 * Provides comprehensive user information for frontend applications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EMSService {

    private final UserService userService;
    private final UserBusinessAppRoleService userBusinessAppRoleService;
    private final DepartmentService departmentService;
    private final HybridAuthorizationService hybridAuthorizationService;
    private final AuthController authController;
    private final UserDepartmentRepository userDepartmentRepository;

    // Action display name mappings
    private static final Map<String, String> ACTION_DISPLAY_NAMES = Map.of(
        "CREATE_CASE", "Create Case",
        "READ_CASE", "View Case", 
        "UPDATE_CASE", "Update Case",
        "DELETE_CASE", "Delete Case",
        "APPROVE_CASE", "Approve Case",
        "ASSIGN_CASE", "Assign Case",
        "CLOSE_CASE", "Close Case",
        "VIEW_QUEUE", "View Queue",
        "CLAIM_TASK", "Claim Task",
        "COMPLETE_TASK", "Complete Task"
    );

    /**
     * Build comprehensive user context for the whoami endpoint
     */
    public WhoAmIResponse buildUserContext(String userId) {
        try {
            log.debug("Building user context for userId: {}", userId);
            
            UUID userUuid = UUID.fromString(userId);
            
            // Get user information
            Optional<User> optionalUser = userService.findById(userUuid);
            if (optionalUser.isEmpty()) {
                return WhoAmIResponse.builder()
                    .success(false)
                    .build();
            }

            User user = optionalUser.get();
            
            // Build user context
            WhoAmIResponse.UserContext userContext = buildUserContext(user);
            
            // Get user roles
            List<WhoAmIResponse.RoleContext> roles = getUserRoles(userUuid);
            
            // Get user departments
            List<WhoAmIResponse.DepartmentContext> departments = getUserDepartments(userUuid);
            
            // Get user permissions (basic implementation)
            List<WhoAmIResponse.PermissionContext> permissions = getUserPermissions(userUuid);
            
            // Get session context
            WhoAmIResponse.SessionContext sessionContext = buildSessionContext(userUuid);

            return WhoAmIResponse.builder()
                .success(true)
                .user(userContext)
                .roles(roles)
                .departments(departments)
                .permissions(permissions)
                .context(sessionContext)
                .build();

        } catch (Exception e) {
            log.error("Error building user context for userId: {}", userId, e);
            return WhoAmIResponse.builder()
                .success(false)
                .build();
        }
    }

    /**
     * Check user authorization for specific resources and actions
     */
    public EMSAuthResponse checkUserAuthorization(String userId, EMSAuthRequest request) {
        try {
            log.debug("Checking authorization for userId: {} with request: {}", userId, request);
            
            long startTime = System.currentTimeMillis();
            
            UUID userUuid = UUID.fromString(userId);
            
            // Validate user exists
            Optional<User> optionalUser = userService.findById(userUuid);
            if (optionalUser.isEmpty()) {
                return EMSAuthResponse.builder()
                    .success(false)
                    .actions(Collections.emptyList())
                    .resourceAccess(buildDefaultResourceAccess(false))
                    .derivedRoles(Collections.emptyList())
                    .evaluationTime(System.currentTimeMillis() - startTime)
                    .build();
            }

            // Perform authorization checks
            List<EMSAuthResponse.ActionAuthResult> actionResults = performActionChecks(userId, request);
            
            // Build resource access summary
            EMSAuthResponse.ResourceAccess resourceAccess = buildResourceAccessSummary(actionResults);
            
            // Get derived roles (simplified for now)
            List<String> derivedRoles = getDerivedRoles(userId);
            
            long evaluationTime = System.currentTimeMillis() - startTime;

            return EMSAuthResponse.builder()
                .success(true)
                .actions(actionResults)
                .resourceAccess(resourceAccess)
                .derivedRoles(derivedRoles)
                .evaluationTime(evaluationTime)
                .build();

        } catch (Exception e) {
            log.error("Error checking authorization for userId: {} with request: {}", userId, request, e);
            return EMSAuthResponse.builder()
                .success(false)
                .actions(Collections.emptyList())
                .resourceAccess(buildDefaultResourceAccess(false))
                .derivedRoles(Collections.emptyList())
                .evaluationTime(0L)
                .build();
        }
    }

    /**
     * Validate user session using AuthController
     */
    public boolean validateUserSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return false;
        }
        
        try {
            var response = authController.validateSession(sessionId);
            return response.getStatusCode().is2xxSuccessful() && 
                   response.getBody() != null &&
                   Boolean.TRUE.equals(((Map<?, ?>) response.getBody()).get("success"));
        } catch (Exception e) {
            log.debug("Session validation failed for sessionId: {}", sessionId, e);
            return false;
        }
    }

    /**
     * Get user ID from session using AuthController
     */
    public Optional<String> getUserIdFromSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return Optional.empty();
        }
        
        try {
            var response = authController.validateSession(sessionId);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
                if (Boolean.TRUE.equals(responseBody.get("success"))) {
                    Map<?, ?> userInfo = (Map<?, ?>) responseBody.get("user");
                    if (userInfo != null) {
                        return Optional.ofNullable((String) userInfo.get("id"));
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get user ID from session: {}", sessionId, e);
        }
        
        return Optional.empty();
    }

    // Private helper methods

    private WhoAmIResponse.UserContext buildUserContext(User user) {
        return WhoAmIResponse.UserContext.builder()
            .id(user.getUserId().toString())
            .username(user.getUsername())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .displayName(user.getFirstName() + " " + user.getLastName())
            .isActive(user.getIsActive())
            .attributes(user.getGlobalAttributes())
            .build();
    }

    private List<WhoAmIResponse.RoleContext> getUserRoles(UUID userId) {
        try {
            List<UserBusinessAppRole> userRoles = userBusinessAppRoleService.getActiveUserRolesByUserId(userId);
            
            return userRoles.stream()
                .map(this::mapToRoleContext)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Error getting user roles for userId: {}", userId, e);
            return Collections.emptyList();
        }
    }

    private WhoAmIResponse.RoleContext mapToRoleContext(UserBusinessAppRole userRole) {
        BusinessAppRole role = userRole.getBusinessAppRole();
        
        return WhoAmIResponse.RoleContext.builder()
            .id(role.getId())
            .roleName(role.getRoleName())
            .displayName(role.getRoleDisplayName())
            .businessApplication(role.getBusinessApplication().getBusinessAppName())
            .isActive(userRole.getIsActive())
            .metadata(role.getMetadata())
            .build();
    }

    private List<WhoAmIResponse.DepartmentContext> getUserDepartments(UUID userId) {
        try {
            List<UserDepartment> userDepartments = userDepartmentRepository.findActiveUserDepartments(userId);
            
            return userDepartments.stream()
                .map(this::mapToDepartmentContext)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Error getting user departments for userId: {}", userId, e);
            return Collections.emptyList();
        }
    }

    private WhoAmIResponse.DepartmentContext mapToDepartmentContext(UserDepartment userDepartment) {
        Department department = userDepartment.getDepartment();
        
        return WhoAmIResponse.DepartmentContext.builder()
            .id(department.getId())
            .name(department.getDepartmentName())
            .code(department.getDepartmentCode())
            .isActive(userDepartment.getIsActive())
            .build();
    }

    private List<WhoAmIResponse.PermissionContext> getUserPermissions(UUID userId) {
        // Basic permission implementation - can be enhanced with actual policy evaluation
        List<WhoAmIResponse.PermissionContext> permissions = new ArrayList<>();
        
        // Get user roles to determine basic permissions
        List<UserBusinessAppRole> userRoles = userBusinessAppRoleService.getActiveUserRolesByUserId(userId);
        
        if (!userRoles.isEmpty()) {
            // Basic permissions based on roles
            Set<String> actions = new HashSet<>();
            
            for (UserBusinessAppRole userRole : userRoles) {
                String roleName = userRole.getBusinessAppRole().getRoleName();
                actions.addAll(getBasicActionsForRole(roleName));
            }
            
            if (!actions.isEmpty()) {
                permissions.add(WhoAmIResponse.PermissionContext.builder()
                    .resource("case")
                    .actions(new ArrayList<>(actions))
                    .build());
            }
        }
        
        return permissions;
    }

    private Set<String> getBasicActionsForRole(String roleName) {
        Set<String> actions = new HashSet<>();
        
        switch (roleName) {
            case "INTAKE_ANALYST":
                actions.addAll(Arrays.asList("create", "read", "update", "route"));
                break;
            case "INVESTIGATOR":
                actions.addAll(Arrays.asList("read", "update", "claim", "complete"));
                break;
            case "INVESTIGATION_MANAGER":
                actions.addAll(Arrays.asList("read", "update", "assign", "approve"));
                break;
            case "ADMIN":
                actions.addAll(Arrays.asList("create", "read", "update", "delete", "approve", "assign"));
                break;
            default:
                actions.add("read");
        }
        
        return actions;
    }

    private WhoAmIResponse.SessionContext buildSessionContext(UUID userId) {
        List<String> queues = getUserQueues(userId);
        
        return WhoAmIResponse.SessionContext.builder()
            .sessionExpiration(Instant.now().plus(30, ChronoUnit.MINUTES).toString())
            .lastAccessed(Instant.now().toString())
            .queues(queues)
            .build();
    }

    private List<String> getUserQueues(UUID userId) {
        List<String> queues = new ArrayList<>();
        
        try {
            List<UserBusinessAppRole> userRoles = userBusinessAppRoleService.getActiveUserRolesByUserId(userId);
            
            for (UserBusinessAppRole userRole : userRoles) {
                BusinessAppRole role = userRole.getBusinessAppRole();
                Map<String, Object> metadata = role.getMetadata();
                
                if (metadata != null && metadata.containsKey("queue")) {
                    String queue = (String) metadata.get("queue");
                    if (queue != null && !queues.contains(queue)) {
                        queues.add(queue);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting user queues for userId: {}", userId, e);
        }
        
        return queues;
    }

    private List<EMSAuthResponse.ActionAuthResult> performActionChecks(String userId, EMSAuthRequest request) {
        List<EMSAuthResponse.ActionAuthResult> results = new ArrayList<>();
        
        // If specific action is requested, check it
        if (request.getActionId() != null) {
            EMSAuthResponse.ActionAuthResult result = checkSingleAction(userId, request);
            results.add(result);
        } else {
            // Check common actions for the resource type
            List<String> commonActions = getCommonActionsForResource(request.getResourceType());
            
            for (String actionId : commonActions) {
                EMSAuthRequest actionRequest = EMSAuthRequest.builder()
                    .resourceId(request.getResourceId())
                    .actionId(actionId)
                    .resourceType(request.getResourceType())
                    .context(request.getContext())
                    .build();
                    
                EMSAuthResponse.ActionAuthResult result = checkSingleAction(userId, actionRequest);
                results.add(result);
            }
        }
        
        return results;
    }

    private EMSAuthResponse.ActionAuthResult checkSingleAction(String userId, EMSAuthRequest request) {
        try {
            // Build authorization check request
            AuthorizationCheckRequest authRequest = buildAuthorizationCheckRequest(userId, request);
            
            // Perform authorization check using existing service
            AuthorizationCheckResponse authResponse = hybridAuthorizationService.checkAuthorization(authRequest);
            
            return EMSAuthResponse.ActionAuthResult.builder()
                .actionId(request.getActionId())
                .displayName(ACTION_DISPLAY_NAMES.getOrDefault(request.getActionId(), request.getActionId()))
                .allowed(authResponse.isAllowed())
                .reason(authResponse.getMessage())
                .build();
                
        } catch (Exception e) {
            log.error("Error checking action {} for userId: {}", request.getActionId(), userId, e);
            
            return EMSAuthResponse.ActionAuthResult.builder()
                .actionId(request.getActionId())
                .displayName(ACTION_DISPLAY_NAMES.getOrDefault(request.getActionId(), request.getActionId()))
                .allowed(false)
                .reason("Authorization check failed: " + e.getMessage())
                .build();
        }
    }

    private AuthorizationCheckRequest buildAuthorizationCheckRequest(String userId, EMSAuthRequest request) {
        AuthorizationCheckRequest.Principal principal = AuthorizationCheckRequest.Principal.builder()
            .id(UUID.fromString(userId))
            .attributes(new HashMap<>())
            .build();
            
        Map<String, Object> resourceAttributes = new HashMap<>();
        if (request.getContext() != null) {
            resourceAttributes.putAll(request.getContext());
        }
        
        AuthorizationCheckRequest.Resource resource = AuthorizationCheckRequest.Resource.builder()
            .kind(request.getResourceType() != null ? request.getResourceType() : "case")
            .id(request.getResourceId() != null ? request.getResourceId() : "unknown")
            .attributes(resourceAttributes)
            .build();
            
        return AuthorizationCheckRequest.builder()
            .principal(principal)
            .resource(resource)
            .action(mapActionId(request.getActionId()))
            .build();
    }

    private String mapActionId(String actionId) {
        if (actionId == null) return "read";
        
        // Map EMS action IDs to Cerbos actions
        switch (actionId.toUpperCase()) {
            case "CREATE_CASE":
                return "create";
            case "READ_CASE":
            case "VIEW_CASE":
                return "read";
            case "UPDATE_CASE":
                return "update";
            case "DELETE_CASE":
                return "delete";
            case "APPROVE_CASE":
                return "approve";
            default:
                return actionId.toLowerCase().replace("_", "-");
        }
    }

    private List<String> getCommonActionsForResource(String resourceType) {
        if (resourceType == null) resourceType = "case";
        
        switch (resourceType.toLowerCase()) {
            case "case":
                return Arrays.asList("CREATE_CASE", "READ_CASE", "UPDATE_CASE", "APPROVE_CASE");
            case "task":
                return Arrays.asList("VIEW_QUEUE", "CLAIM_TASK", "COMPLETE_TASK");
            case "workflow":
                return Arrays.asList("VIEW_QUEUE", "CLAIM_TASK", "COMPLETE_TASK");
            default:
                return Arrays.asList("READ_CASE");
        }
    }

    private EMSAuthResponse.ResourceAccess buildResourceAccessSummary(List<EMSAuthResponse.ActionAuthResult> actionResults) {
        boolean canRead = actionResults.stream()
            .anyMatch(action -> ("READ_CASE".equals(action.getActionId()) || "read".equals(action.getActionId())) 
                && action.getAllowed());
                
        boolean canWrite = actionResults.stream()
            .anyMatch(action -> ("UPDATE_CASE".equals(action.getActionId()) || "CREATE_CASE".equals(action.getActionId())) 
                && action.getAllowed());
                
        boolean canDelete = actionResults.stream()
            .anyMatch(action -> "DELETE_CASE".equals(action.getActionId()) && action.getAllowed());
            
        boolean canApprove = actionResults.stream()
            .anyMatch(action -> "APPROVE_CASE".equals(action.getActionId()) && action.getAllowed());
        
        return EMSAuthResponse.ResourceAccess.builder()
            .canRead(canRead)
            .canWrite(canWrite)
            .canDelete(canDelete)
            .canApprove(canApprove)
            .build();
    }

    private EMSAuthResponse.ResourceAccess buildDefaultResourceAccess(boolean defaultValue) {
        return EMSAuthResponse.ResourceAccess.builder()
            .canRead(defaultValue)
            .canWrite(defaultValue)
            .canDelete(defaultValue)
            .canApprove(defaultValue)
            .build();
    }

    private List<String> getDerivedRoles(String userId) {
        // Simplified implementation - could be enhanced with actual Cerbos derived role evaluation
        List<String> derivedRoles = new ArrayList<>();
        
        try {
            UUID userUuid = UUID.fromString(userId);
            List<UserBusinessAppRole> userRoles = userBusinessAppRoleService.getActiveUserRolesByUserId(userUuid);
            
            for (UserBusinessAppRole userRole : userRoles) {
                String roleName = userRole.getBusinessAppRole().getRoleName();
                derivedRoles.add("user"); // Base role
                
                // Add role-specific derived roles
                switch (roleName) {
                    case "INTAKE_ANALYST":
                        derivedRoles.add("case_creator");
                        break;
                    case "INVESTIGATOR":
                        derivedRoles.add("case_assignee");
                        break;
                    case "INVESTIGATION_MANAGER":
                        derivedRoles.add("case_supervisor");
                        break;
                }
            }
        } catch (Exception e) {
            log.error("Error getting derived roles for userId: {}", userId, e);
        }
        
        return derivedRoles.stream().distinct().collect(Collectors.toList());
    }
}