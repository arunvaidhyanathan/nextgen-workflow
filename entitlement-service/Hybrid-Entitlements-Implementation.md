# Hybrid Entitlements Implementation Guide
**Version**: 2.0  
**Last Updated**: 2025-01-08  
**Project**: NextGen Workflow Entitlement Service  

## 🎯 Executive Summary

This document provides a comprehensive implementation guide for the NextGen Workflow Hybrid Entitlement System. The system integrates **Cerbos ABAC (Attribute-Based Access Control)** with **Database RBAC (Role-Based Access Control)**, providing flexible, high-performance authorization that can seamlessly switch between engines based on configuration.

### Key Features Implemented
- **Hybrid Authorization Engine**: Seamless switching between Cerbos ABAC and Database RBAC
- **BPMN-Aware Policies**: Authorization integrated with workflow phases and task states
- **Generic Resource Permissions**: Flexible ABAC for case-specific access grants
- **Enhanced Derived Roles**: Dynamic role assignment based on context and attributes
- **Time-Based Access Control**: Business hours and emergency access patterns
- **Comprehensive Audit Trail**: Complete logging for compliance and security monitoring

## 🏛️ Architecture Overview

### System Components

```
NextGen Workflow Hybrid Entitlement System
│
├── 🔄 Hybrid Authorization Service
│   ├── AuthorizationEngine Interface
│   ├── CerbosAuthorizationEngine (ABAC)
│   ├── DatabaseAuthorizationEngine (RBAC)
│   └── Engine Selection Logic
│
├── 📊 Database Schema (13 Tables)
│   ├── Core Identity: entitlement_core_users
│   ├── Cerbos Support: business_applications, user_business_app_roles
│   ├── Database Engine: entitlement_domain_roles, entitlement_permissions
│   ├── Resource ABAC: resource_permissions
│   └── Audit: entitlement_audit_logs
│
├── 🛡️ Cerbos Policy Suite (Version 2.0)
│   ├── Enhanced Derived Roles
│   ├── Hybrid Case Resource Policies
│   ├── Advanced Workflow Policies
│   ├── NextGen Enhanced Policies
│   └── Principal Base Policies
│
└── 🔗 Integration Points
    ├── OneCMS Service Authorization
    ├── Flowable Workflow Engine
    ├── API Gateway Session Validation
    └── Cross-Service Authorization Checks
```

### Engine Selection Flow

```
Authorization Request
│
├── Configuration Check
│   ├── authorization.engine.use-cerbos=true  → Cerbos ABAC Engine
│   └── authorization.engine.use-cerbos=false → Database RBAC Engine
│
├── Principal Building
│   ├── Core Attributes (departments, queues, roles)
│   ├── Database Permissions (hybrid mode)
│   ├── Resource-Level Permissions (ABAC)
│   └── Workflow Context (BPMN integration)
│
├── Authorization Decision
│   ├── Policy Evaluation
│   ├── Derived Role Assignment
│   ├── Condition Checking
│   └── Decision (ALLOW/DENY + Reasoning)
│
└── Audit Logging
    ├── Decision Details
    ├── Engine Type Used
    ├── Principal Context
    └── Resource Information
```

## 🛡️ Cerbos Policy Implementation

### 1. Enhanced Derived Roles (`derived_roles/one-cms.yaml`)

#### Core Resource-Level Derived Roles
```yaml
# Traditional resource-based roles
- name: case_assignee              # User assigned to specific case
- name: case_department_member     # User in case's department
- name: queue_member              # User has queue access
- name: task_assignee             # User assigned to current task
- name: same_department_member    # User in same department as case
```

#### BPMN Workflow Phase-Based Roles
```yaml
# Dynamic workflow-aware roles
- name: intake_phase_authorized      # EO Intake operations
- name: routing_phase_authorized     # EO Head/Officer routing
- name: department_review_authorized # CSIS/ER/Legal review
- name: investigation_phase_authorized # Investigation operations
```

#### Management and Oversight Roles
```yaml
# Cross-departmental management access
- name: cross_department_manager     # Senior management override
- name: queue_supervisor            # Queue management capabilities
- name: audit_authorized           # Audit and compliance access
```

#### Hybrid System Integration Roles
```yaml
# Database integration support
- name: database_authorized         # Database permission validation
- name: resource_level_authorized   # Resource-specific ABAC permissions
```

#### Conditional Access Roles
```yaml
# Time and condition-based access
- name: business_hours_user         # Business hours restriction
- name: emergency_access_user       # Emergency override capabilities
```

### 2. Hybrid Case Resource Policy (`resources/case.yaml`)

#### Key Authorization Points Mapped to BPMN

| BPMN Task | Actions | Authorized Roles | Derived Roles | Conditions |
|-----------|---------|------------------|---------------|------------|
| `task_create_case` | create, initiate, draft | GROUP_EO_INTAKE_ANALYST | intake_phase_authorized | workflowPhase == "INTAKE" |
| `task_fill_information` | update, add_entities, add_allegations | GROUP_EO_INTAKE_ANALYST | task_assignee, case_assignee | Current assignee OR intake phase |
| `task_assign_case` | assign, assign_to_officer | GROUP_EO_HEAD | routing_phase_authorized | workflowPhase == "ROUTING" |
| `task_officer_routing` | route, send_to_department | GROUP_EO_OFFICER | routing_phase_authorized | EO Officer with routing authority |
| `task_dept_review` | review, add_details | GROUP_CSIS_INTAKE_ANALYST, etc. | department_review_authorized | Department alignment |
| `task_manager_assignment` | assign_investigator | GROUP_INVESTIGATION_MANAGER | investigation_phase_authorized | Investigation manager queues |
| `task_investigator_work` | investigate, create_plan | GROUP_INVESTIGATOR | task_assignee, case_assignee | Assigned investigator |
| `task_active_investigation` | conduct_investigation | GROUP_INVESTIGATOR | investigation_phase_authorized | Active investigation phase |

#### Advanced Features

**Generic Resource-Level Permissions (ABAC Override)**:
```yaml
- actions: ["*"]
  effect: EFFECT_ALLOW
  roles: ["user"]
  derivedRoles: [resource_level_authorized]
  condition:
    match:
      all:
        - expr: has(request.principal.attr.resourcePermissions)
        - expr: request.action in resourcePermissions[caseId].allowedActions
        - expr: now() < resourcePermissions[caseId].expiresAt
```

**Emergency Access Override**:
```yaml
- actions: ["emergency_access"]
  effect: EFFECT_ALLOW
  derivedRoles: [emergency_access_user]
  condition:
    match:
      any:
        - expr: request.resource.attr.priority == "URGENT"
        - expr: request.resource.attr.isEmergency == true
        - expr: has(request.principal.attr.emergencyAccess)
```

### 3. Advanced Workflow Policy (`resources/one-cms-workflow.yaml`)

#### BPMN Candidate Group Integration
```yaml
# Task claiming with candidate group verification
- actions: ["claim_task"]
  condition:
    match:
      all:
        - expr: request.resource.attr.currentTask.assignee == null
        - expr: request.resource.attr.currentTask.queue in request.principal.attr.queues
        # BPMN candidate group alignment check
        - expr: |
            !has(request.resource.attr.currentTask.candidateGroups) ||
            size(request.resource.attr.currentTask.candidateGroups.filter(group, 
              has(request.principal.attr.roles[group]))) > 0
```

#### Queue Management and Supervision
```yaml
# Enhanced queue access with supervision
- actions: ["manage_queue", "rebalance_queue"]
  roles: ["GROUP_INVESTIGATION_MANAGER", "GROUP_EO_OFFICER"]
  derivedRoles: [queue_supervisor]
  condition:
    match:
      expr: |
        request.resource.attr.currentQueue in request.principal.attr.queues ||
        request.resource.attr.currentQueue in request.principal.attr.supervisedQueues
```

#### Task Delegation with Business Rules
```yaml
# Enhanced delegation with constraints
- actions: ["delegate_task", "reassign_task"]
  condition:
    match:
      all:
        - expr: # Manager or current assignee
        - expr: # Can't delegate to same person (business rule)
                request.resource.attr.targetAssignee != request.resource.attr.currentTask.assignee
```

### 4. NextGen Enhanced Policies

#### Advanced Case Management (`resources/case-nextgen.yaml`)
- **Bulk Operations**: Batch processing with size limits and management oversight
- **Workflow Phase Transitions**: Dynamic phase advancement with proper authorization
- **Collaboration and Sharing**: Case sharing with resource-level permission grants
- **Time-Based Operations**: Business hours restrictions with emergency overrides

#### Advanced Workflow Management (`resources/workflow-nextgen.yaml`)
- **SLA and Priority Management**: Priority setting with business rules
- **Batch Operations**: Bulk task operations with reasonable limits
- **Analytics and Reporting**: Role-based reporting scope
- **Integration API Access**: Service account and integration user support

### 5. Enhanced Principal Policy (`principal/user-base.yaml`)

#### Base User Capabilities
- **Session Management**: Enhanced session validation with expiry checks
- **Profile Management**: Self-service profile updates with business hours restrictions
- **Department Access**: Baseline departmental information access
- **Notification System**: User notification management
- **Audit Trail**: Personal audit trail access
- **Rate Limiting**: Comment and report generation rate limiting

## 🗄️ Database Schema Implementation

### Core Identity Table
```sql
-- Single source of truth for user identity
CREATE TABLE entitlements.entitlement_core_users (
    user_id UUID PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    global_attributes JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
```

### Hybrid Engine Support Tables

#### Database Engine (RBAC)
```sql
-- Application domains for multi-tenancy
CREATE TABLE entitlements.entitlement_application_domains (
    domain_id UUID PRIMARY KEY,
    domain_name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    is_tiered BOOLEAN DEFAULT false,
    domain_metadata JSONB DEFAULT '{}'
);

-- Domain-specific roles
CREATE TABLE entitlements.entitlement_domain_roles (
    role_id UUID PRIMARY KEY,
    domain_id UUID REFERENCES entitlement_application_domains(domain_id),
    role_name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    role_level VARCHAR(50), -- Tier1, Tier2, Manager, Admin
    maker_checker_type VARCHAR(50)
);

-- Granular permissions
CREATE TABLE entitlements.entitlement_permissions (
    permission_id UUID PRIMARY KEY,
    resource_type VARCHAR(255) NOT NULL, -- case, workflow, task, queue
    action VARCHAR(255) NOT NULL,        -- create, read, update, delete, claim, complete
    description TEXT,
    UNIQUE(resource_type, action)
);
```

#### Resource-Level ABAC Support
```sql
-- Generic resource-level permissions
CREATE TABLE entitlements.resource_permissions (
    resource_permission_id UUID PRIMARY KEY,
    user_id UUID REFERENCES entitlement_core_users(user_id),
    resource_type VARCHAR(255) NOT NULL,  -- case, workflow, task, queue
    resource_id VARCHAR(255) NOT NULL,    -- specific resource identifier
    allowed_actions TEXT[] NOT NULL,      -- [read, update, comment, etc.]
    conditions JSONB DEFAULT '{}',        -- time-based, location-based conditions
    granted_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ,               -- optional expiration
    is_active BOOLEAN DEFAULT true,
    granted_by UUID REFERENCES entitlement_core_users(user_id)
);
```

#### Cerbos Engine Support
```sql
-- Business applications (existing)
CREATE TABLE entitlements.business_applications (
    id BIGSERIAL PRIMARY KEY,
    business_app_name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT true
);

-- Business application roles (existing)
CREATE TABLE entitlements.business_app_roles (
    id BIGSERIAL PRIMARY KEY,
    business_app_id BIGINT REFERENCES business_applications(id),
    role_name VARCHAR(100) NOT NULL,
    role_display_name VARCHAR(255) NOT NULL,
    metadata JSONB DEFAULT '{}' -- Contains queue mappings
);
```

### Queue Management Integration
```sql
-- Queue to role mappings stored in business_app_roles.metadata
{
  "queues": [
    "eo-head-queue",
    "eo-officer-queue", 
    "intake-analyst-queue",
    "investigation-manager-queue",
    "investigator-queue",
    "csis-intake-analyst-queue",
    "er-intake-analyst-queue",
    "legal-intake-analyst-queue"
  ],
  "supervisedQueues": [
    "investigator-queue"  // For managers
  ]
}
```

## 🔗 Service Integration Implementation

### 1. Enhanced Principal Building

```java
@Component
public class HybridPrincipalBuilder {
    
    public AuthorizationCheckRequest.Principal buildPrincipal(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        
        Map<String, Object> attributes = new HashMap<>();
        
        // Core attributes
        attributes.put("isActive", user.getIsActive());
        attributes.put("departments", getUserDepartments(userId));
        attributes.put("queues", getUserQueues(userId));
        
        // Hybrid mode enhancements
        if (isHybridMode()) {
            attributes.put("databasePermissions", getDatabasePermissions(userId));
            attributes.put("resourcePermissions", getResourcePermissions(userId));
            attributes.put("supervisedQueues", getSupervisedQueues(userId));
        }
        
        // Workflow context
        attributes.put("currentWorkflowPhase", getCurrentWorkflowPhase(userId));
        attributes.put("emergencyAccess", hasEmergencyAccess(userId));
        
        return AuthorizationCheckRequest.Principal.builder()
            .id(userId.toString())
            .roles(getUserRoles(userId))
            .attributes(attributes)
            .build();
    }
    
    private Map<String, ResourcePermissionSummary> getResourcePermissions(UUID userId) {
        return resourcePermissionRepository.findActiveByUserId(userId)
            .stream()
            .collect(Collectors.toMap(
                perm -> perm.getResourceType() + "::" + perm.getResourceId(),
                perm -> ResourcePermissionSummary.builder()
                    .allowedActions(Arrays.asList(perm.getAllowedActions()))
                    .conditions(perm.getConditions())
                    .expiresAt(perm.getExpiresAt())
                    .build()
            ));
    }
}
```

### 2. Generic Resource Permission Service

```java
@Service
@Transactional
public class HybridResourcePermissionService {
    
    /**
     * Grant generic resource-level permission (not hardcoded)
     * Examples:
     * - Consultant access: grantResourcePermission(consultantId, "case", caseNumber, ["read", "comment"], conditions, expiry)
     * - Temporary investigator: grantResourcePermission(tempId, "queue", "investigation-queue", ["view", "claim"], null, expiry)
     * - External reviewer: grantResourcePermission(reviewerId, "task", taskId, ["review", "approve"], conditions, expiry)
     */
    public void grantResourcePermission(
            UUID userId, 
            String resourceType,     // Generic: case, workflow, task, queue
            String resourceId,       // Specific: CMS-2025-000001, task-123, queue-name
            List<String> allowedActions,
            Map<String, Object> conditions,
            Instant expiresAt) {
        
        ResourcePermission permission = ResourcePermission.builder()
            .userId(userId)
            .resourceType(resourceType)
            .resourceId(resourceId)
            .allowedActions(allowedActions.toArray(new String[0]))
            .conditions(JsonMapper.toJsonb(conditions))
            .expiresAt(expiresAt)
            .isActive(true)
            .grantedAt(Instant.now())
            .grantedBy(getCurrentUserId())
            .build();
            
        resourcePermissionRepository.save(permission);
        
        // Invalidate authorization cache
        authorizationCacheService.evictUserCache(userId);
        
        // Audit log
        auditResourcePermissionGrant(userId, resourceType, resourceId, allowedActions);
    }
    
    /**
     * Revoke resource-level permission
     */
    public void revokeResourcePermission(UUID userId, String resourceType, String resourceId) {
        resourcePermissionRepository.deactivatePermission(userId, resourceType, resourceId);
        authorizationCacheService.evictUserCache(userId);
        auditResourcePermissionRevoke(userId, resourceType, resourceId);
    }
    
    /**
     * Check if user has specific resource-level permission
     */
    public boolean hasResourcePermission(UUID userId, String resourceType, 
                                       String resourceId, String action) {
        return resourcePermissionRepository
            .findActivePermissions(userId, resourceType, resourceId)
            .stream()
            .anyMatch(perm -> 
                Arrays.asList(perm.getAllowedActions()).contains(action) &&
                evaluateConditions(perm.getConditions())
            );
    }
    
    private boolean evaluateConditions(JsonNode conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        
        // Time-based conditions
        if (conditions.has("timeRestriction")) {
            JsonNode timeRestr = conditions.get("timeRestriction");
            LocalTime now = LocalTime.now();
            LocalTime start = LocalTime.parse(timeRestr.get("startTime").asText());
            LocalTime end = LocalTime.parse(timeRestr.get("endTime").asText());
            if (now.isBefore(start) || now.isAfter(end)) {
                return false;
            }
        }
        
        // Location-based conditions
        if (conditions.has("locationRestriction")) {
            // Implement IP-based or VPN-based location checking
            return evaluateLocationCondition(conditions.get("locationRestriction"));
        }
        
        return true;
    }
}
```

### 3. BPMN Integration Service

```java
@Component
public class WorkflowAuthorizationIntegrator {
    
    /**
     * Build workflow-aware authorization request
     */
    public AuthorizationCheckRequest buildWorkflowAuthorizationRequest(
            UUID userId, String processInstanceId, String taskId, String action) {
        
        // Get workflow context from Flowable
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
            .processInstanceId(processInstanceId).singleResult();
        
        Task currentTask = taskService.createTaskQuery()
            .taskId(taskId).singleResult();
        
        // Build resource with workflow context
        Map<String, Object> resourceAttributes = new HashMap<>();
        resourceAttributes.put("processInstanceId", processInstanceId);
        resourceAttributes.put("workflowPhase", getWorkflowPhase(processInstance));
        resourceAttributes.put("currentQueue", getCurrentQueue(currentTask));
        
        if (currentTask != null) {
            Map<String, Object> taskInfo = new HashMap<>();
            taskInfo.put("assignee", currentTask.getAssignee());
            taskInfo.put("candidateGroups", getCandidateGroups(currentTask));
            taskInfo.put("queue", getCurrentQueue(currentTask));
            taskInfo.put("status", getTaskStatus(currentTask));
            resourceAttributes.put("currentTask", taskInfo);
        }
        
        return AuthorizationCheckRequest.builder()
            .requestId(UUID.randomUUID().toString())
            .principal(principalBuilder.buildPrincipal(userId))
            .resource(AuthorizationCheckRequest.Resource.builder()
                .kind("OneCMS::oneCmsCaseWorkflow")
                .id(processInstanceId)
                .attributes(resourceAttributes)
                .build())
            .action(action)
            .build();
    }
    
    private String getWorkflowPhase(ProcessInstance processInstance) {
        // Map current activity to workflow phase
        String currentActivityId = getCurrentActivityId(processInstance);
        
        switch (currentActivityId) {
            case "task_create_case":
            case "task_fill_information":
                return "INTAKE";
            case "task_assign_case":
            case "task_officer_routing":
                return "ROUTING";
            case "task_dept_review":
            case "task_add_details":
            case "task_er_routing":
                return "DEPT_REVIEW";
            case "task_manager_assignment":
            case "task_investigator_work":
            case "task_active_investigation":
                return "INVESTIGATION";
            default:
                return "UNKNOWN";
        }
    }
    
    private List<String> getCandidateGroups(Task task) {
        return taskService.getIdentityLinksForTask(task.getId())
            .stream()
            .filter(link -> IdentityLinkType.CANDIDATE.equals(link.getType()))
            .map(IdentityLink::getGroupId)
            .collect(Collectors.toList());
    }
    
    private String getCurrentQueue(Task task) {
        // Map candidate groups to queue names
        List<String> candidateGroups = getCandidateGroups(task);
        
        // Queue mapping logic
        Map<String, String> groupToQueue = Map.of(
            "GROUP_EO_INTAKE_ANALYST", "eo-intake-queue",
            "GROUP_EO_HEAD", "eo-head-queue",
            "GROUP_EO_OFFICER", "eo-officer-queue",
            "GROUP_CSIS_INTAKE_ANALYST", "csis-intake-analyst-queue",
            "GROUP_ER_INTAKE_ANALYST", "er-intake-analyst-queue",
            "GROUP_LEGAL_INTAKE_ANALYST", "legal-intake-analyst-queue",
            "GROUP_INVESTIGATION_MANAGER", "investigation-manager-queue",
            "GROUP_INVESTIGATOR", "investigator-queue"
        );
        
        return candidateGroups.stream()
            .map(groupToQueue::get)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse("default-queue");
    }
}
```

### 4. OneCMS Service Integration

```java
@RestController
@RequestMapping("/api/cms/v1/cases")
public class CaseManagementController {
    
    @PostMapping
    public ResponseEntity<CaseResponseDto> createCase(
            @RequestBody CreateCaseRequest request,
            HttpServletRequest httpRequest) {
        
        UUID userId = extractUserIdFromHeader(httpRequest);
        
        // 1. Authorization check before case creation
        AuthorizationCheckRequest authRequest = AuthorizationCheckRequest.builder()
            .requestId(UUID.randomUUID().toString())
            .principal(principalBuilder.buildPrincipal(userId))
            .resource(AuthorizationCheckRequest.Resource.builder()
                .kind("case")
                .id("new-case")  // For creation
                .attributes(Map.of(
                    "workflowPhase", "INTAKE",
                    "department_code", request.getDepartmentCode()
                ))
                .build())
            .action("create")
            .build();
        
        AuthorizationCheckResponse authResponse = 
            authorizationService.checkAuthorization(authRequest);
        
        if (!authResponse.isAllowed()) {
            throw new ForbiddenException("Not authorized to create cases: " + 
                authResponse.getDenialReason());
        }
        
        // 2. Create case with workflow integration
        CaseDto createdCase = caseService.createCase(request, userId);
        
        // 3. Start workflow process
        WorkflowStartResponse workflowResponse = 
            workflowServiceClient.startProcess("oneCmsCaseWorkflow", 
                Map.of("caseId", createdCase.getCaseId()));
        
        // 4. Return response with workflow info
        return ResponseEntity.ok(CaseResponseDto.builder()
            .caseId(createdCase.getCaseId())
            .caseNumber(createdCase.getCaseNumber())
            .processInstanceId(workflowResponse.getProcessInstanceId())
            .initialTaskId(workflowResponse.getInitialTaskId())
            .workflowStatus("STARTED")
            .build());
    }
    
    @PutMapping("/{caseNumber}")
    public ResponseEntity<CaseDto> updateCase(
            @PathVariable String caseNumber,
            @RequestBody UpdateCaseRequest request,
            HttpServletRequest httpRequest) {
        
        UUID userId = extractUserIdFromHeader(httpRequest);
        CaseDto existingCase = caseService.getCaseByNumber(caseNumber);
        
        // Authorization check with full case context
        AuthorizationCheckRequest authRequest = buildCaseAuthorizationRequest(
            userId, existingCase, "update");
        
        AuthorizationCheckResponse authResponse = 
            authorizationService.checkAuthorization(authRequest);
        
        if (!authResponse.isAllowed()) {
            throw new ForbiddenException("Not authorized to update case: " + 
                authResponse.getDenialReason());
        }
        
        // Perform update
        CaseDto updatedCase = caseService.updateCase(existingCase.getCaseId(), request, userId);
        
        return ResponseEntity.ok(updatedCase);
    }
    
    private AuthorizationCheckRequest buildCaseAuthorizationRequest(
            UUID userId, CaseDto caseDto, String action) {
        
        Map<String, Object> resourceAttributes = new HashMap<>();
        resourceAttributes.put("assigneeId", caseDto.getAssigneeId());
        resourceAttributes.put("department_code", caseDto.getDepartmentCode());
        resourceAttributes.put("workflowPhase", caseDto.getWorkflowPhase());
        resourceAttributes.put("status", caseDto.getStatus());
        
        // Add current task info if available
        if (caseDto.getCurrentTaskId() != null) {
            Task currentTask = getCurrentTaskFromWorkflow(caseDto.getCurrentTaskId());
            if (currentTask != null) {
                Map<String, Object> taskInfo = new HashMap<>();
                taskInfo.put("assignee", currentTask.getAssignee());
                taskInfo.put("queue", getCurrentQueue(currentTask));
                taskInfo.put("candidateGroups", getCandidateGroups(currentTask));
                resourceAttributes.put("currentTask", taskInfo);
            }
        }
        
        return AuthorizationCheckRequest.builder()
            .requestId(UUID.randomUUID().toString())
            .principal(principalBuilder.buildPrincipal(userId))
            .resource(AuthorizationCheckRequest.Resource.builder()
                .kind("case")
                .id(caseDto.getCaseNumber())
                .attributes(resourceAttributes)
                .build())
            .action(action)
            .build();
    }
}
```

## 🔄 Engine Configuration and Switching

### Application Configuration
```yaml
# application.yml
authorization:
  engine:
    use-cerbos: false                    # Switch to database engine
    allow-runtime-switching: true       # Enable runtime switching (for testing)
    cache:
      enabled: true
      ttl-seconds: 300                  # 5-minute cache
      max-size: 10000
  
  database:
    permissions:
      preload: true                     # Preload permissions for performance
      cache-refresh-interval: 600       # 10-minute refresh
  
  cerbos:
    host: localhost
    port: 3593
    tls:
      enabled: false
    policy-cache-ttl: 300
```

### Engine Selection Logic
```java
@Configuration
public class HybridAuthorizationConfig {
    
    @Bean
    @Primary
    public AuthorizationEngine authorizationEngine(
            CerbosAuthorizationEngine cerbosEngine,
            DatabaseAuthorizationEngine databaseEngine,
            @Value("${authorization.engine.use-cerbos:false}") boolean useCerbos) {
        
        if (useCerbos) {
            log.info("Initializing hybrid authorization system with Cerbos ABAC engine");
            return cerbosEngine;
        } else {
            log.info("Initializing hybrid authorization system with Database RBAC engine");
            return databaseEngine;
        }
    }
    
    @Bean
    @ConditionalOnProperty(
        value = "authorization.engine.allow-runtime-switching", 
        havingValue = "true")
    public RuntimeEngineSwitcher engineSwitcher(
            CerbosAuthorizationEngine cerbosEngine,
            DatabaseAuthorizationEngine databaseEngine) {
        return new RuntimeEngineSwitcher(cerbosEngine, databaseEngine);
    }
}
```

## 📊 Performance and Caching

### Authorization Decision Caching
```java
@Service
public class CachedAuthorizationService {
    
    @Cacheable(value = "authorization-decisions", 
               key = "#request.principal.id + ':' + #request.resource.kind + ':' + #request.resource.id + ':' + #request.action",
               condition = "#request.resource.kind != 'audit_log'") // Don't cache audit decisions
    public AuthorizationCheckResponse checkAuthorization(AuthorizationCheckRequest request) {
        return authorizationEngine.checkAuthorization(request);
    }
    
    @CacheEvict(value = "authorization-decisions", 
                key = "#userId + ':*'", 
                allEntries = true)
    public void evictUserCache(UUID userId) {
        log.debug("Evicted authorization cache for user: {}", userId);
    }
    
    @Scheduled(fixedRate = 600000) // 10 minutes
    @CacheEvict(value = "authorization-decisions", allEntries = true)
    public void refreshAuthorizationCache() {
        log.info("Refreshed authorization decision cache");
    }
}
```

### Database Query Optimization
```sql
-- Optimized indexes for authorization queries
CREATE INDEX CONCURRENTLY idx_resource_permissions_user_resource 
ON entitlements.resource_permissions(user_id, resource_type, resource_id) 
WHERE is_active = true;

CREATE INDEX CONCURRENTLY idx_resource_permissions_expiry 
ON entitlements.resource_permissions(expires_at) 
WHERE expires_at IS NOT NULL AND is_active = true;

CREATE INDEX CONCURRENTLY idx_user_domain_roles_active 
ON entitlements.entitlement_user_domain_roles(user_id, role_id) 
WHERE is_active = true;

-- Partial indexes for performance
CREATE INDEX CONCURRENTLY idx_audit_logs_recent 
ON entitlements.entitlement_audit_logs(event_timestamp DESC, user_id) 
WHERE event_timestamp >= (CURRENT_TIMESTAMP - INTERVAL '30 days');
```

## 🔍 Monitoring and Auditing

### Comprehensive Audit Logging
```java
@Component
@Async
public class AuthorizationAuditService {
    
    @EventListener
    public void logAuthorizationDecision(AuthorizationDecisionEvent event) {
        EntitlementAuditLog auditLog = EntitlementAuditLog.builder()
            .eventType("AUTHORIZATION_CHECK")
            .userId(UUID.fromString(event.getPrincipalId()))
            .resourceType(event.getResourceKind())
            .resourceId(event.getResourceId())
            .action(event.getAction())
            .decision(event.isAllowed() ? "ALLOW" : "DENY")
            .decisionReason(event.getDenialReason())
            .engineType(event.getEngineType())
            .requestMetadata(JsonMapper.toJsonb(event.getRequestContext()))
            .responseMetadata(JsonMapper.toJsonb(event.getResponseContext()))
            .sessionId(event.getSessionId())
            .ipAddress(event.getIpAddress())
            .userAgent(event.getUserAgent())
            .build();
            
        auditLogRepository.save(auditLog);
    }
    
    @EventListener
    public void logResourcePermissionGrant(ResourcePermissionGrantEvent event) {
        EntitlementAuditLog auditLog = EntitlementAuditLog.builder()
            .eventType("RESOURCE_PERMISSION_GRANT")
            .userId(event.getTargetUserId())
            .resourceType(event.getResourceType())
            .resourceId(event.getResourceId())
            .action("GRANT_ACCESS")
            .decision("ALLOW")
            .decisionReason("Direct resource permission granted")
            .engineType("HYBRID")
            .requestMetadata(JsonMapper.toJsonb(Map.of(
                "grantedBy", event.getGrantedBy(),
                "allowedActions", event.getAllowedActions(),
                "expiresAt", event.getExpiresAt()
            )))
            .build();
            
        auditLogRepository.save(auditLog);
    }
}
```

### Performance Metrics
```java
@Component
public class AuthorizationMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter authorizationRequestsCounter;
    private final Counter authorizationDenialsCounter;
    private final Timer authorizationTimer;
    
    public AuthorizationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.authorizationRequestsCounter = Counter.builder("authorization.requests.total")
            .description("Total authorization requests")
            .tag("engine", "hybrid")
            .register(meterRegistry);
            
        this.authorizationDenialsCounter = Counter.builder("authorization.denials.total")
            .description("Total authorization denials")
            .register(meterRegistry);
            
        this.authorizationTimer = Timer.builder("authorization.request.duration")
            .description("Authorization request processing time")
            .register(meterRegistry);
    }
    
    @EventListener
    public void recordAuthorizationMetrics(AuthorizationDecisionEvent event) {
        authorizationRequestsCounter.increment(
            Tags.of(
                "resource_type", event.getResourceKind(),
                "action", event.getAction(),
                "engine_type", event.getEngineType(),
                "decision", event.isAllowed() ? "allow" : "deny"
            )
        );
        
        if (!event.isAllowed()) {
            authorizationDenialsCounter.increment(
                Tags.of(
                    "resource_type", event.getResourceKind(),
                    "denial_reason", event.getDenialReason()
                )
            );
        }
        
        authorizationTimer.record(event.getProcessingTime(), TimeUnit.MILLISECONDS);
    }
}
```

## 🧪 Testing Strategy

### Policy Testing
```java
@TestMethodOrder(OrderAnnotation.class)
@SpringBootTest
class HybridAuthorizationIntegrationTest {
    
    @Test
    @Order(1)
    void testCaseCreationAuthorization() {
        // Given: EO Intake Analyst user
        UUID userId = createTestUser("alice.intake", Set.of("GROUP_EO_INTAKE_ANALYST"));
        
        // When: Check authorization to create case
        AuthorizationCheckResponse response = authorizationService.checkAuthorization(
            buildCaseAuthorizationRequest(userId, "new-case", "create")
        );
        
        // Then: Should be allowed
        assertThat(response.isAllowed()).isTrue();
        assertThat(response.getDecisionBasis()).contains("intake_phase_authorized");
    }
    
    @Test
    @Order(2)
    void testWorkflowTaskClaiming() {
        // Given: User with queue access
        UUID userId = createTestUser("bob.investigator", 
            Set.of("GROUP_INVESTIGATOR"), 
            Set.of("investigator-queue"));
        
        // When: Check authorization to claim task
        AuthorizationCheckResponse response = authorizationService.checkAuthorization(
            buildWorkflowAuthorizationRequest(userId, "task-123", "claim_task")
        );
        
        // Then: Should be allowed if task is in their queue
        assertThat(response.isAllowed()).isTrue();
    }
    
    @Test
    @Order(3)
    void testResourceLevelPermission() {
        // Given: User with direct resource permission
        UUID userId = createTestUser("consultant.external", Set.of());
        String caseNumber = "CMS-2025-000001";
        
        // Grant direct resource permission
        resourcePermissionService.grantResourcePermission(
            userId, "case", caseNumber, 
            List.of("read", "add_comment"), 
            null, 
            Instant.now().plus(7, ChronoUnit.DAYS)
        );
        
        // When: Check authorization with resource permission
        AuthorizationCheckResponse response = authorizationService.checkAuthorization(
            buildCaseAuthorizationRequest(userId, caseNumber, "read")
        );
        
        // Then: Should be allowed via resource-level permission
        assertThat(response.isAllowed()).isTrue();
        assertThat(response.getDecisionBasis()).contains("resource_level_authorized");
    }
    
    @Test
    @Order(4)
    void testEngineSwitch() {
        // Given: Test user and case
        UUID userId = createTestUser("test.user", Set.of("GROUP_EO_OFFICER"));
        String caseNumber = "CMS-2025-000002";
        
        // When: Test with Cerbos engine
        configureEngine("cerbos");
        AuthorizationCheckResponse cerbosResponse = authorizationService.checkAuthorization(
            buildCaseAuthorizationRequest(userId, caseNumber, "update")
        );
        
        // When: Test with Database engine
        configureEngine("database");
        AuthorizationCheckResponse databaseResponse = authorizationService.checkAuthorization(
            buildCaseAuthorizationRequest(userId, caseNumber, "update")
        );
        
        // Then: Both engines should return same decision for same scenario
        assertThat(cerbosResponse.isAllowed()).isEqualTo(databaseResponse.isAllowed());
    }
    
    @Test
    @Order(5)
    void testEmergencyAccess() {
        // Given: Regular user with emergency access flag
        UUID userId = createTestUser("emergency.user", Set.of());
        grantEmergencyAccess(userId);
        
        // When: Check emergency access to urgent case
        AuthorizationCheckResponse response = authorizationService.checkAuthorization(
            buildEmergencyAuthorizationRequest(userId, "urgent-case", "emergency_access")
        );
        
        // Then: Should be allowed via emergency access
        assertThat(response.isAllowed()).isTrue();
        assertThat(response.getDecisionBasis()).contains("emergency_access_user");
    }
}
```

## 🚀 Deployment and Migration

### Migration Steps

#### Phase 1: Policy Deployment
```bash
# 1. Deploy enhanced Cerbos policies
curl -X POST http://localhost:3593/admin/policy \
  -H "Content-Type: application/json" \
  -d @policies/derived_roles/one-cms.yaml

curl -X POST http://localhost:3593/admin/policy \
  -H "Content-Type: application/json" \
  -d @policies/resources/case.yaml

# 2. Validate policy syntax
curl -X POST http://localhost:3593/admin/policy/validate \
  -H "Content-Type: application/json" \
  -d @policies/resources/one-cms-workflow.yaml
```

#### Phase 2: Database Schema Migration
```sql
-- Execute hybrid schema migration
\i db/changelog/001-hybrid-schema.xml

-- Populate initial data
\i db/changelog/002-sample-data.xml

-- Add performance indexes
\i db/migrations/003-performance-indexes.sql
```

#### Phase 3: Service Configuration
```yaml
# Update application.yml for hybrid mode
authorization:
  engine:
    use-cerbos: true  # Start with Cerbos, can switch to database later
    cache:
      enabled: true
      ttl-seconds: 300
```

#### Phase 4: Validation Testing
```bash
# Test authorization endpoints
curl -X POST http://localhost:8081/api/entitlements/check \
  -H "Content-Type: application/json" \
  -H "X-User-Id: user-uuid" \
  -d '{
    "resourceKind": "case",
    "resourceId": "CMS-2025-000001",
    "action": "update"
  }'

# Test engine switching
curl -X POST http://localhost:8081/api/entitlements/system/engine/switch \
  -H "Content-Type: application/json" \
  -d '{"engine": "database"}'
```

## 📚 API Reference

### Core Authorization Endpoint
```http
POST /api/entitlements/check
Content-Type: application/json
X-User-Id: 550e8400-e29b-41d4-a716-446655440001

{
  "resourceKind": "case",
  "resourceId": "CMS-2025-000001",
  "action": "update",
  "context": {
    "workflowPhase": "INVESTIGATION",
    "currentTask": {
      "assignee": "550e8400-e29b-41d4-a716-446655440001",
      "queue": "investigator-queue"
    }
  }
}
```

Response:
```json
{
  "allowed": true,
  "decisionBasis": "task_assignee",
  "engineType": "CERBOS",
  "processingTime": 45,
  "denialReason": null,
  "additionalInfo": {
    "derivedRoles": ["task_assignee", "investigation_phase_authorized"],
    "appliedRules": ["case.yaml:task_assignee_rule"],
    "principalContext": {
      "departments": ["SECURITY"],
      "queues": ["investigator-queue"],
      "roles": ["GROUP_INVESTIGATOR"]
    }
  }
}
```

### Resource Permission Management
```http
POST /api/entitlements/resource-permissions
Content-Type: application/json
X-User-Id: admin-user-id

{
  "targetUserId": "consultant-user-id",
  "resourceType": "case",
  "resourceId": "CMS-2025-000123",
  "allowedActions": ["read", "add_comment"],
  "conditions": {
    "timeRestriction": {
      "startTime": "08:00",
      "endTime": "18:00"
    }
  },
  "expiresAt": "2025-01-15T23:59:59Z"
}
```

### Engine Status and Control
```http
GET /api/entitlements/system/engine/status
```

Response:
```json
{
  "currentEngine": "CERBOS",
  "isHealthy": true,
  "switchingEnabled": true,
  "lastSwitched": "2025-01-08T10:00:00Z",
  "performance": {
    "avgResponseTime": 42,
    "successRate": 99.8,
    "cacheHitRate": 85.2
  }
}
```

## 🎯 Success Metrics and KPIs

### Performance Targets
- **Authorization Decision Time**: < 500ms (95th percentile)
- **Cache Hit Rate**: > 80%
- **System Availability**: 99.9%
- **Engine Switch Time**: < 5 seconds

### Compliance Metrics
- **Audit Log Completeness**: 100% of authorization decisions logged
- **Policy Coverage**: All BPMN tasks have corresponding authorization checks
- **Access Review**: Quarterly review of resource-level permissions
- **Failed Access Monitoring**: Real-time alerting on authorization failures

### Business Metrics
- **User Productivity**: Reduction in access-related support tickets
- **Process Efficiency**: Faster case processing due to proper authorization
- **Security Posture**: Reduced unauthorized access incidents
- **System Flexibility**: Successful engine switching without service disruption

## 🔧 Troubleshooting Guide

### Common Issues

#### Authorization Denied Unexpectedly
1. Check user's role assignments in database
2. Verify queue memberships in `business_app_roles.metadata`
3. Review Cerbos policy syntax and derived role conditions
4. Check for expired resource-level permissions
5. Validate workflow phase context in authorization request

#### Performance Issues
1. Monitor cache hit rates and adjust TTL settings
2. Review database query performance with `EXPLAIN ANALYZE`
3. Check Cerbos server response times
4. Optimize principal building queries
5. Consider increasing authorization cache size

#### Engine Switching Problems
1. Verify both engines are healthy before switching
2. Check configuration property updates
3. Monitor for consistent authorization decisions post-switch
4. Validate cache clearing after engine switch
5. Review audit logs for decision discrepancies

### Debug Tools

#### Policy Testing CLI
```bash
# Test specific policy scenario
./test-policy.sh \
  --user-id "user-uuid" \
  --resource-type "case" \
  --resource-id "CMS-2025-000001" \
  --action "update" \
  --engine "cerbos"
```

#### Authorization Decision Explainer
```bash
# Get detailed authorization explanation
curl -X POST http://localhost:8081/api/entitlements/check/explain \
  -H "Content-Type: application/json" \
  -d @authorization-request.json
```

## 📞 Support and Maintenance

### Monitoring Setup
- **Authorization Decision Metrics**: Grafana dashboard with success/failure rates
- **Performance Monitoring**: Response time percentiles and cache hit rates  
- **Security Alerts**: Failed authorization attempts and policy violations
- **System Health**: Engine status and database connectivity

### Regular Maintenance
- **Weekly**: Review authorization failure logs and patterns
- **Monthly**: Audit resource-level permissions for cleanup
- **Quarterly**: Performance optimization and policy review
- **Annually**: Complete security audit and compliance review

---

**Document Version**: 2.0  
**Last Updated**: 2025-01-08  
**Next Review**: 2025-04-08  
**Maintained By**: NextGen Workflow Development Team