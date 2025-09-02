# How Cerbos Works in NextGen Workflow Architecture

## Overview

The NextGen Workflow system implements a **Hybrid Authorization Architecture** that combines traditional Role-Based Access Control (RBAC) with advanced Policy-Based Access Control (ABAC) using the Cerbos policy engine. This document explains how authorization works across the microservices ecosystem.

## The Core Principle: Cerbos is Stateless

Cerbos is a pure, stateless decision engine that answers one question:

**"Given this principal (user), trying to perform this action, on this resource... is it allowed?"**

Cerbos stores **no user data** - it only holds policy rules. The Entitlement Service acts as the "Principal Builder" that constructs rich user context from the database and feeds it to Cerbos for policy evaluation.

## Architecture Components

### 1. Entitlement Service (Port 8081)
- **Purpose**: Central authorization service and principal builder
- **Technology**: Spring Boot with hybrid authorization engines
- **Database**: PostgreSQL `entitlements` schema
- **Key Features**:
  - Session-based authentication (replaced JWT)
  - Hybrid authorization engine (Database + Cerbos)
  - Dynamic principal building from database
  - Complete audit trail for compliance

### 2. Cerbos Policy Engine
- **Integration**: Cerbos SDK with gRPC communication
- **Policies Location**: `/policies` directory with derived roles
- **Key Policy Files**:
  - `derived_roles/one-cms.yaml` - Dynamic role definitions
  - `resources/case.yaml` - Case resource permissions
  - `resources/one-cms-workflow.yaml` - Workflow task permissions

### 3. Domain Services Integration
- **OneCMS Service**: Case management with workflow integration
- **Workflow Service**: Task and process management
- **API Gateway**: Session validation and user context injection

## Complete Authorization Flow: Task Claim Example

### Real-World Integration Flow

```
+----------------------------------------+
| 1. OneCMS Service (Port 8083)          |
|    - User 'alice.intake' tries to      |
|      claim Task 'task-intake-001'      |
|    - AuthorizationService.checkAuth()  |
|    - EntitlementServiceClient (Circuit |
|      Breaker Protected)               |
+------------------+--------------------+
                   |
                   | 2. POST /api/entitlements/check
                   |    Headers: X-User-Id: alice.intake
                   |    Payload:
                   |    {
                   |      "principalId": "alice.intake",
                   |      "resource": {
                   |        "kind": "one-cms-workflow",
                   |        "id": "task-intake-001",
                   |        "attr": {
                   |          "currentTask": {
                   |            "queue": "intake-analyst-queue",
                   |            "processInstanceId": "proc-inst-456"
                   |          },
                   |          "case": {
                   |            "departmentCode": "INTAKE",
                   |            "assignedTo": null
                   |          }
                   |        }
                   |      },
                   |      "action": "claim_task"
                   |    }
                   |
                   v
+------------------+--------------------+
| 3. Entitlement Service (Port 8081)    |
|    HybridAuthorizationService         |
+----------------------------------------+
    |
    | 3a. Engine Selection:
    |     if (useCerbos && cerbosAvailable) → CerbosEngine
    |     else → DatabaseEngine
    |
    | 3b. Principal Building Queries:
    |     SELECT u.*, u.attributes FROM entitlement_core_users u WHERE u.username = 'alice.intake'
    |     --> Gets user attributes: {"region": "US", "experience_level": "senior"}
    |
    | 3c. SELECT d.department_code FROM departments d 
    |     JOIN user_departments ud ON d.id = ud.department_id 
    |     WHERE ud.user_id = 'alice.intake'
    |     --> Gets departments: ["INTAKE", "INVESTIGATION"]
    |
    | 3d. SELECT r.role_name, r.metadata FROM business_app_roles r
    |     JOIN user_business_app_roles ur ON r.id = ur.role_id
    |     WHERE ur.user_id = 'alice.intake' AND r.app_name = 'OneCMS'
    |     --> Gets roles: ["INTAKE_ANALYST"]
    |     --> Gets metadata.queues: ["intake-analyst-queue", "eo-officer-queue"]
    |
    | 3e. **Constructs Rich Principal Object:**
    |     {
    |       "id": "alice.intake",
    |       "roles": ["INTAKE_ANALYST"],
    |       "attr": {
    |         "departments": ["INTAKE", "INVESTIGATION"],
    |         "queues": ["intake-analyst-queue", "eo-officer-queue"],
    |         "region": "US",
    |         "experience_level": "senior"
    |       }
    |     }
    |
    v
+------------------+--------------------+
| 4. Cerbos SDK Call (gRPC)             |
|    CerbosAuthorizationEngine          |
|    - Principal.newBuilder()           |
|      .setId("alice.intake")           |
|      .addRoles("INTAKE_ANALYST")      |
|      .putAttributes(...)              |
|    - Resource.newBuilder()            |
|      .setKind("one-cms-workflow")     |
|      .setId("task-intake-001")        |
|      .putAttributes(...)              |
|    - Action: "claim_task"             |
+------------------+--------------------+
                   |
                   | 5. gRPC call to Cerbos PDP
                   |    dev.cerbos.sdk.CheckResourcesRequest
                   |
                   v
+------------------+--------------------+
| 6. Cerbos PDP (Policy Engine)         |
|    External Cerbos instance/container  |
+----------------------------------------+
    |
    | 6a. **Load Policies:**
    |     - Imports `derived_roles/one-cms.yaml`
    |     - Loads `resources/one-cms-workflow.yaml`
    |
    | 6b. **Evaluate Derived Roles:**
    |     queue_member: 
    |       expr: "request.resource.attr.currentTask.queue in request.principal.attr.queues"
    |       Evaluates: "intake-analyst-queue" in ["intake-analyst-queue", "eo-officer-queue"]
    |       Result: TRUE → Alice gets temporary `queue_member` derived role
    |
    | 6c. **Policy Rule Evaluation:**
    |     Action: claim_task
    |     Rules:
    |       - effect: EFFECT_ALLOW
    |         derivedRoles: ["queue_member"]
    |       Match: Alice has queue_member → ALLOW
    |
    | 6d. **Decision:** EFFECT_ALLOW
    |
    v
+------------------+--------------------+
| 7. Cerbos returns CheckResult          |
|    isAllowed: true                     |
|    validationErrors: []                |
+------------------+--------------------+
                   |
                   | 8. Response to Entitlement Service
                   |
                   v
+------------------+--------------------+
| 9. Entitlement Service Response        |
|    - Logs decision to audit table     |
|    - Returns: {"allowed": true}       |
+------------------+--------------------+
                   |
                   | 10. HTTP 200 OK Response
                   |
                   v
+------------------+--------------------+
| 11. OneCMS Service                     |
|     - Authorization check passed      |
|     - Proceeds with WorkflowClient    |
|       .claimTask(taskId, userId)      |
|     - Updates case assignment         |
+----------------------------------------+
```

## Detailed Technical Implementation

### 1. Service-to-Service Communication Pattern

**OneCMS Service Authorization Integration:**
```java
@RestController
public class CaseManagementController {
    
    @Autowired
    private AuthorizationService authorizationService;
    
    @PostMapping("/api/cms/v1/cases/{caseNumber}/tasks/{taskId}/claim")
    public ResponseEntity<?> claimTask(
            @PathVariable String taskId,
            @RequestHeader("X-User-Id") String userId) {
        
        // Build authorization check request
        AuthorizationCheckRequest authRequest = AuthorizationCheckRequest.builder()
            .principalId(userId)
            .resource(ResourceBuilder.workflow()
                .id(taskId)
                .addAttribute("currentTask", taskAttributes)
                .addAttribute("case", caseAttributes)
                .build())
            .action("claim_task")
            .build();
        
        // Check authorization via EntitlementServiceClient
        if (!authorizationService.checkAuthorization(authRequest)) {
            return ResponseEntity.status(403).body("Access denied");
        }
        
        // Proceed with business logic
        return workflowServiceClient.claimTask(taskId, userId);
    }
}
```

### 2. Principal Building Process (Entitlement Service)

**HybridAuthorizationService Core Logic:**
```java
@Service
public class HybridAuthorizationService {
    
    @Value("${authorization.engine.use-cerbos:true}")
    private boolean useCerbos;
    
    public AuthorizationCheckResponse checkAuthorization(AuthorizationCheckRequest request) {
        try {
            if (useCerbos && cerbosAuthorizationEngine.isAvailable()) {
                return cerbosAuthorizationEngine.checkAuthorization(request);
            }
        } catch (Exception e) {
            log.warn("Cerbos engine failed, falling back to database engine", e);
        }
        
        // Fallback to database engine
        return databaseAuthorizationEngine.checkAuthorization(request);
    }
}
```

**Principal Building Queries:**
```sql
-- 1. Get User Base Attributes
SELECT u.user_id, u.username, u.email, u.first_name, u.last_name, 
       u.attributes as user_attributes, u.is_active
FROM entitlement_core_users u 
WHERE u.username = 'alice.intake';

-- 2. Get User Departments
SELECT d.department_code, d.department_name
FROM departments d
JOIN user_departments ud ON d.department_id = ud.department_id
WHERE ud.user_id = (SELECT user_id FROM entitlement_core_users WHERE username = 'alice.intake');

-- 3. Get Business Application Roles and Queues
SELECT r.role_name, r.metadata, ba.app_name
FROM business_app_roles r
JOIN user_business_app_roles ur ON r.role_id = ur.role_id
JOIN business_applications ba ON r.app_id = ba.app_id
WHERE ur.user_id = (SELECT user_id FROM entitlement_core_users WHERE username = 'alice.intake')
  AND ba.app_name = 'OneCMS';

-- Result: role_name='INTAKE_ANALYST', metadata.queues=['intake-analyst-queue','eo-officer-queue']
```

### 3. Authorization Engine Implementations

**CerbosAuthorizationEngine:**
```java
@Component
public class CerbosAuthorizationEngine implements AuthorizationEngine {
    
    private final CerbosBlockingStub cerbosClient;
    
    @Override
    public AuthorizationCheckResponse checkAuthorization(AuthorizationCheckRequest request) {
        // Build Cerbos Principal
        Principal.Builder principalBuilder = Principal.newBuilder()
            .setId(request.getPrincipalId());
        
        // Add static roles
        userRoles.forEach(principalBuilder::addRoles);
        
        // Add attributes for derived roles
        principalBuilder.putAllAttributes(
            AttributeValue.newBuilder()
                .putAttributeValues("departments", departments)
                .putAttributeValues("queues", queues)
                .putAttributeValues("region", userAttributes.get("region"))
                .build().getAttributeValuesMap()
        );
        
        // Build Resource
        Resource resource = Resource.newBuilder()
            .setKind(request.getResource().getKind())
            .setId(request.getResource().getId())
            .putAllAttributes(resourceAttributes)
            .build();
        
        // Make authorization check
        CheckResourcesRequest cerbosRequest = CheckResourcesRequest.newBuilder()
            .setPrincipal(principalBuilder.build())
            .addResources(ResourceEntry.newBuilder()
                .setResource(resource)
                .addActions(request.getAction())
                .build())
            .build();
        
        CheckResourcesResponse response = cerbosClient.checkResources(cerbosRequest);
        
        boolean allowed = response.getResultsList().stream()
            .anyMatch(result -> result.getIsAllowed());
        
        return AuthorizationCheckResponse.builder()
            .allowed(allowed)
            .engine("cerbos")
            .build();
    }
}
```

### 4. Database Schema Integration

**Core Authorization Tables:**
```sql
-- Primary user identity with JSONB attributes
CREATE TABLE entitlement_core_users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255),
    attributes JSONB DEFAULT '{}', -- Flexible user metadata
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Department assignments for derived roles
CREATE TABLE user_departments (
    user_id UUID REFERENCES entitlement_core_users(user_id),
    department_id UUID REFERENCES departments(department_id),
    is_primary BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Application-specific role assignments
CREATE TABLE user_business_app_roles (
    user_id UUID REFERENCES entitlement_core_users(user_id),
    role_id UUID REFERENCES business_app_roles(role_id),
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Role definitions with queue metadata
CREATE TABLE business_app_roles (
    role_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    app_id UUID REFERENCES business_applications(app_id),
    role_name VARCHAR(100) NOT NULL,
    metadata JSONB DEFAULT '{}', -- Contains queue assignments
    is_active BOOLEAN DEFAULT true
);
```

### 5. Cerbos Policy Structure

**Derived Roles Definition** (`/policies/derived_roles/one-cms.yaml`):
```yaml
apiVersion: api.cerbos.dev/v1
derivedRoles:
  one_cms_derived_roles:
    definitions:
      # Dynamic queue membership
      queue_member:
        parentRoles: ["user"]
        condition:
          match:
            expr: "request.resource.attr.currentTask.queue in request.principal.attr.queues"
      
      # Case department membership  
      case_department_member:
        parentRoles: ["user"]
        condition:
          match:
            expr: "request.resource.attr.case.departmentCode in request.principal.attr.departments"
      
      # Task assignment ownership
      task_assignee:
        parentRoles: ["user"]
        condition:
          match:
            expr: "request.resource.attr.currentTask.assignedTo == request.principal.id"
      
      # Case ownership
      case_assignee:
        parentRoles: ["user"]
        condition:
          match:
            expr: "request.resource.attr.case.assignedTo == request.principal.id"
```

**Resource Policy** (`/policies/resources/one-cms-workflow.yaml`):
```yaml
apiVersion: api.cerbos.dev/v1
resourcePolicy:
  version: "default"
  resource: "one-cms-workflow"
  importDerivedRoles:
    - one_cms_derived_roles
  
  rules:
    # Task claiming - requires queue membership
    - actions: ["claim_task"]
      effect: EFFECT_ALLOW
      derivedRoles:
        - queue_member
    
    # Task completion - requires task assignment
    - actions: ["complete_task"]
      effect: EFFECT_ALLOW
      derivedRoles:
        - task_assignee
    
    # Case reading - department or assignment based
    - actions: ["read_case"]
      effect: EFFECT_ALLOW
      derivedRoles:
        - case_department_member
        - case_assignee
    
    # Case creation - specific roles only
    - actions: ["create_case"]
      effect: EFFECT_ALLOW
      roles:
        - INTAKE_ANALYST
        - EO_OFFICER
```

## Understanding Derived Roles: Dynamic Authorization

### The Core Principle: Derived Roles are Dynamic Aliases

Derived roles are **temporary, computed labels** that Cerbos assigns to a principal during policy evaluation. They're not stored in your database - they're calculated on-the-fly based on the relationship between the user and the specific resource being accessed.

**Key Concept**: A user doesn't "have" a derived role permanently. They **qualify** for it during a specific authorization check based on context.

### Derived Role Evaluation Process

**Question Answered**: "During this specific check, does this user qualify as a `case_department_member` for this particular resource?"

**Dynamic Calculation**:
```
User Context + Resource Context + Business Rules = Temporary Derived Role Assignment
```

**Example Scenarios**:
- Alice accesses Case-001 (HR dept) → Gets `case_department_member` role 
- Alice accesses Case-002 (Legal dept) → Does NOT get `case_department_member` role 
- Same user, different resources, different derived role assignments

## Current Implementation Status

### Completed Components
- **Hybrid Authorization Service**: Engine selection and fallback logic
- **Database Schema**: Complete user, role, and department entities
- **Principal Building Logic**: Database queries and context construction
- **Cerbos SDK Integration**: gRPC client configuration and policy evaluation
- **Session Authentication**: X-Session-Id and X-User-Id header handling
- **Circuit Breaker Pattern**: Resilient service communication
- **Audit Logging**: Complete authorization decision tracking
- **Policy Definitions**: Derived roles and resource policies

### Missing Components
- **Authorization REST Endpoint**: `/api/entitlements/check` endpoint not implemented
- **Complete Database Queries**: Some placeholder implementations need real data
- **Production Session Store**: Currently using in-memory sessions (development only)

### Key Configuration

**Entitlement Service Properties:**
```yaml
# Enable/disable Cerbos engine
authorization:
  engine:
    use-cerbos: true

# Cerbos connection
cerbos:
  target: localhost:3593
  tls-enabled: false

# Fallback engine settings
database:
  engine:
    cache-duration: PT30M
```

## Business Role to Queue Mappings

| Business Role | Queue Assignment | Workflow Tasks |
|---------------|------------------|----------------|
| `INTAKE_ANALYST` | `intake-analyst-queue`, `eo-officer-queue` | Case creation, initial review |
| `EO_HEAD` | `eo-head-queue` | Case assignment oversight |
| `EO_OFFICER` | `eo-officer-queue`, `eo-head-queue` | Case routing and triage |
| `CSIS_INTAKE_ANALYST` | `csis-intake-analyst-queue` | Security case review |
| `ER_INTAKE_ANALYST` | `er-intake-analyst-queue` | Employee relations review |
| `LEGAL_INTAKE_ANALYST` | `legal-intake-analyst-queue` | Legal compliance review |
| `INVESTIGATION_MANAGER` | `investigation-manager-queue` | Investigation assignment |
| `INVESTIGATOR` | `investigator-queue` | Investigation execution |

## Key Authorization Patterns

### 1. Queue-Based Task Access
**Pattern**: Users can only claim tasks from queues they're assigned to
**Implementation**: `queue_member` derived role with queue membership check
**Policy**: `request.resource.attr.currentTask.queue in request.principal.attr.queues`

### 2. Department-Based Case Access  
**Pattern**: Users can access cases in their department
**Implementation**: `case_department_member` derived role
**Policy**: `request.resource.attr.case.departmentCode in request.principal.attr.departments`

### 3. Assignment-Based Ownership
**Pattern**: Users can access resources assigned to them
**Implementation**: `task_assignee`, `case_assignee` derived roles
**Policy**: `request.resource.attr.assignedTo == request.principal.id`

### 4. Role-Based Creation Rights
**Pattern**: Only specific roles can create certain resources
**Implementation**: Static role checking
**Policy**: Direct role membership (e.g., `INTAKE_ANALYST` can create cases)

## Advanced Authorization Patterns

### Multi-Level Authorization Checks

The OneCMS workflow implements **layered authorization** that checks multiple levels:

```java
// Example: Case creation with workflow start
@PostMapping("/api/cms/v1/cases")
public ResponseEntity<CaseResponse> createCase(
        @RequestBody CreateCaseRequest request,
        @RequestHeader("X-User-Id") String userId) {
    
    // Level 1: Can user create cases?
    authorizationService.checkCaseAuthorization(userId, null, "create_case");
    
    // Level 2: Can user start workflows?
    authorizationService.checkWorkflowAuthorization(userId, processKey, "start_process");
    
    // Level 3: Can user access assigned queues?
    authorizationService.checkQueueAuthorization(userId, initialQueue, "access_queue");
    
    // Proceed with case creation and workflow start
    return caseService.createCaseWithWorkflow(request, userId);
}
```

### Context-Aware Authorization

**Resource Attribute Injection:**
The system automatically injects relevant context into authorization checks:

```java
// Case context includes:
{
  "case": {
    "departmentCode": "HR",
    "assignedTo": "alice.intake",
    "priority": "HIGH",
    "status": "OPEN"
  },
  "currentTask": {
    "queue": "intake-analyst-queue",
    "assignedTo": null,
    "taskName": "Review Case Details"
  },
  "workflow": {
    "processDefinitionKey": "oneCmsCleanCaseWorkflow",
    "processInstanceId": "proc-inst-789"
  }
}
```

### Dynamic Queue Assignment

**Role Metadata Pattern:**
```json
{
  "role_name": "INTAKE_ANALYST",
  "metadata": {
    "queues": [
      "intake-analyst-queue",
      "eo-officer-queue"
    ],
    "permissions": {
      "case_creation": true,
      "department_routing": true
    },
    "attributes": {
      "clearance_level": "standard",
      "max_case_priority": "HIGH"
    }
  }
}
```

## Integration with OneCMS Clean Case Workflow

### Workflow-Authorization Mapping

| Workflow Stage | User Groups | Authorization Checks |
|----------------|-------------|----------------------|
| **Case Creation** | `GROUP_EO_INTAKE_ANALYST` | `create_case`, `access_queue:eo-intake-queue` |
| **EO Head Assignment** | `GROUP_EO_HEAD` | `assign_case`, `access_queue:eo-head-queue` |
| **EO Officer Routing** | `GROUP_EO_OFFICER` | `route_case`, `access_multiple_queues` |
| **Department Review** | Dynamic (`${departmentGroup}`) | `review_case`, `access_queue:${departmentQueue}` |
| **ER Sub-routing** | `GROUP_ER_INTAKE_ANALYST` | `route_case`, `access_queue:er-intake-analyst-queue` |
| **Investigation** | `GROUP_INVESTIGATION_MANAGER`, `GROUP_INVESTIGATOR` | `assign_investigator`, `claim_task`, `complete_task` |

### Derived Role Usage in Workflow

**1. Queue Member Access:**
```yaml
# User can claim tasks from queues they're assigned to
queue_member:
  condition:
    match:
      expr: "request.resource.attr.currentTask.queue in request.principal.attr.queues"
```

**2. Department-Based Case Access:**
```yaml
# User can access cases in their department
case_department_member:
  condition:
    match:
      expr: "request.resource.attr.case.departmentCode in request.principal.attr.departments"
```

**3. Task Assignment Ownership:**
```yaml
# User can complete tasks assigned to them
task_assignee:
  condition:
    match:
      expr: "request.resource.attr.currentTask.assignedTo == request.principal.id"
```

## Error Handling and Resilience

### Circuit Breaker Pattern
```java
@CircuitBreaker(name = "entitlement-service")
@Retryable(value = {Exception.class}, maxAttempts = 3)
public AuthorizationCheckResponse checkAuthorization(AuthorizationCheckRequest request) {
    // Authorization logic with automatic fallback
}
```

### Fallback Strategies
1. **Cerbos Unavailable**: Automatic fallback to database engine
2. **Database Issues**: Cached principal information
3. **Network Failures**: Circuit breaker prevents cascade failures
4. **Invalid Policies**: Detailed error logging and safe defaults

## Why This Architecture Excels

**RBAC + ABAC Hybrid Benefits:**
- **RBAC**: Simple, static role assignments in database (`INTAKE_ANALYST`, `INVESTIGATOR`)
- **ABAC**: Dynamic, context-aware permissions using derived roles
- **Policy Clarity**: Complex logic encapsulated in readable derived role names
- **Reusability**: Derived roles used across multiple resource types
- **Auditability**: Clear decision trail with business-meaningful role names

**Example Policy Readability:**
```yaml
# Clean, readable policy using derived roles
- actions: ["claim_task"]
  effect: EFFECT_ALLOW
  derivedRoles:
    - queue_member

# vs. complex inline conditions (harder to read/maintain)
- actions: ["claim_task"]
  effect: EFFECT_ALLOW
  condition:
    match:
      expr: "request.resource.attr.currentTask.queue in request.principal.attr.queues"
```

**Engineering Benefits:**
- **Maintainable Policies**: Business rules clearly expressed
- **Testable Logic**: Derived roles can be unit tested independently
- **Performance**: Cerbos evaluates policies efficiently
- **Flexibility**: Easy to add new derived roles for future requirements
- **Compliance**: Complete audit trail with meaningful role context

## Summary: Why This Architecture Works

**Separation of Concerns:**
- **Domain Services**: Focus on business logic, delegate authorization
- **Entitlement Service**: Centralized authorization with context building
- **Cerbos Engine**: Pure policy evaluation without user data storage

**Scalability Benefits:**
- **Stateless Authorization**: No session state in authorization layer
- **Caching Strategy**: User context cached for performance
- **Horizontal Scaling**: Stateless services scale independently
- **Policy Changes**: Update policies without service restarts

**Security Features:**
- **Principle of Least Privilege**: Fine-grained permissions
- **Dynamic Authorization**: Context-aware decision making
- **Complete Audit Trail**: All decisions logged for compliance
- **Defense in Depth**: Multiple authorization layers with fallbacks

**The Flow Summary:**
```
Client Request → API Gateway (Session Auth) → Domain Service → 
Entitlement Service (Principal Builder) → Cerbos Engine (Policy Eval) → 
Decision → Domain Service → Business Logic Execution
```

## Next Steps for Implementation

### Critical Missing Component

**Authorization REST Controller:**
```java
@RestController
@RequestMapping("/api/entitlements")
public class AuthorizationController {
    
    @Autowired
    private HybridAuthorizationService hybridAuthorizationService;
    
    @PostMapping("/check")
    public ResponseEntity<AuthorizationCheckResponse> checkAuthorization(
            @RequestBody AuthorizationCheckRequest request) {
        
        AuthorizationCheckResponse response = 
            hybridAuthorizationService.checkAuthorization(request);
        
        return ResponseEntity.ok(response);
    }
}
```

### Integration Testing Strategy

**Test Scenarios:**
1. **Queue Access**: Verify users can only claim tasks from assigned queues
2. **Department Access**: Confirm department-based case visibility
3. **Workflow Progression**: Test authorization at each workflow stage
4. **Derived Role Evaluation**: Validate dynamic role assignment logic
5. **Engine Fallback**: Test database engine when Cerbos unavailable

### Performance Optimization

**Caching Strategy:**
- **Principal Context**: Cache user roles/departments for 30 minutes
- **Policy Evaluation**: Cerbos handles policy caching internally
- **Circuit Breaker**: Fail-fast when authorization service unavailable
- **Batch Checks**: Support multiple resource checks in single call

This architecture provides enterprise-grade authorization with the flexibility to handle complex, context-aware business rules while maintaining performance and auditability.