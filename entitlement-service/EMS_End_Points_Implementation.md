# Implementation Plan: EMS v1 Endpoints for Entitlement Service

## Overview
Adding two new endpoints under `/api/ems/v1/` namespace for frontend authorization support:
- `GET /api/ems/v1/whoami` - Get current user context
- `POST /api/ems/v1/caniuse` - Check user permissions for resources/actions

## Current Service Analysis

**Existing Patterns:**
- **Session-based auth**: Uses `X-Session-Id` or `X-User-Id` headers  
- **Structured responses**: Consistent error/success patterns
- **Swagger integration**: Full OpenAPI 3.0 documentation
- **Service layer delegation**: Controllers → Services → Repositories
- **Error handling**: Try-catch with proper HTTP status codes

## Endpoint Specifications

### 1. GET `/api/ems/v1/whoami`

**Purpose**: Provide comprehensive user context for frontend applications

**Request Headers:**
- `X-Session-Id` (optional) - Session-based auth
- `X-User-Id` (optional) - Direct user ID (API Gateway style)

**Response Structure:**
```typescript
interface WhoAmIResponse {
  success: boolean;
  user: {
    id: string;                    // UUID
    username: string;              // alice.intake
    email: string;                 // alice.intake@company.com  
    firstName: string;             // Alice
    lastName: string;              // Johnson
    displayName: string;           // Alice Johnson
    isActive: boolean;             // true
    attributes: Record<string, any>; // Global attributes
  };
  roles: Array<{
    id: number;                    // Role ID
    roleName: string;              // INTAKE_ANALYST
    displayName: string;           // Intake Analyst
    businessApplication: string;   // onecms
    isActive: boolean;            // true
    metadata: Record<string, any>; // Queue info, etc.
  }>;
  departments: Array<{
    id: number;                    // Department ID
    name: string;                  // Investigation Unit
    code: string;                  // IU
    isActive: boolean;            // true
  }>;
  permissions: Array<{
    resource: string;              // case, workflow, task
    actions: string[];             // [create, read, update]
  }>;
  context: {
    sessionExpiration?: string;    // ISO timestamp
    lastAccessed?: string;         // ISO timestamp  
    queues: string[];             // Available queues
  };
}
```

### 2. POST `/api/ems/v1/caniuse`

**Purpose**: Check user authorization for specific resources/actions

**Request Body:**
```typescript
interface AuthRequest {
  resourceId?: string;    // CMS-10-20045
  actionId?: string;      // CREATE_CASE
  // Extended for comprehensive checks:
  resourceType?: string;  // case, workflow, task
  context?: Record<string, any>; // Additional context
}
```

**Response Structure:**
```typescript
interface AuthResponse {
  success: boolean;
  actions: Array<{
    actionId: string;       // CREATE_CASE
    displayName: string;    // Create Case
    allowed: boolean;       // true/false
    reason?: string;        // Policy evaluation reason
  }>;
  // Enhanced response for frontend:
  resourceAccess: {
    canRead: boolean;
    canWrite: boolean;
    canDelete: boolean;
    canApprove: boolean;
  };
  derivedRoles: string[];   // Applied derived roles
  evaluationTime: number;   // Policy evaluation time in ms
}
```

## Implementation Architecture

### 1. New Controller: `EMSController.java`

**Location**: `src/main/java/com/workflow/entitlements/controller/EMSController.java`

**Key Features:**
- New controller under `/api/ems/v1` namespace  
- Session and header-based authentication support
- Comprehensive error handling
- Swagger/OpenAPI annotations
- Integration with existing services

**Controller Structure:**
```java
@RestController
@RequestMapping("/api/ems/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "EMS", description = "Enterprise Management System API")
public class EMSController {
    
    private final UserService userService;
    private final HybridAuthorizationService authorizationService;
    private final BusinessAppRoleService roleService;
    private final DepartmentService departmentService;
    private final AuthController authController; // For session validation
    
    @GetMapping("/whoami")
    // Implementation...
    
    @PostMapping("/caniuse")  
    // Implementation...
}
```

### 2. New DTOs

**Location**: `src/main/java/com/workflow/entitlements/dto/ems/`

**New Classes:**
- `WhoAmIResponse.java` - Complete user context
- `EMSAuthRequest.java` - Authorization check request
- `EMSAuthResponse.java` - Enhanced authorization response
- `UserContextDTO.java` - User information subset
- `RoleContextDTO.java` - Role information subset
- `DepartmentContextDTO.java` - Department information subset

### 3. Enhanced Service Layer

**New Service**: `EMSService.java`

**Responsibilities:**
- Aggregate user context from multiple sources
- Build comprehensive permission matrices
- Cache frequently accessed user data  
- Handle session validation logic
- Integrate with existing authorization engines

**Service Methods:**
```java
@Service
public class EMSService {
    
    WhoAmIResponse buildUserContext(String userId);
    EMSAuthResponse checkUserAuthorization(String userId, EMSAuthRequest request);
    List<String> getUserQueues(String userId);
    Map<String, List<String>> getUserPermissionMatrix(String userId);
    boolean validateUserSession(String sessionId);
}
```

### 4. Authentication Strategy

**Header Support:**
- `X-Session-Id` - Primary authentication method
- `X-User-Id` - Fallback for API Gateway integration
- Session validation reuse from existing `AuthController`

**User Resolution Flow:**
```java
private String resolveUserId(String sessionId, String directUserId) {
    if (sessionId != null) {
        return validateSessionAndGetUserId(sessionId);
    }
    if (directUserId != null) {
        return validateDirectUserId(directUserId);
    }
    throw new UnauthorizedException("No valid authentication provided");
}
```

## OpenAPI Specification Updates

### 1. New Path Definitions

**File**: `entitlement-service-openapi.yml`

**Additions:**
```yaml
paths:
  /api/ems/v1/whoami:
    get:
      tags:
        - EMS
      summary: Get current user context
      description: |
        Retrieve comprehensive user information including roles, departments, 
        and permissions for frontend application use.
      parameters:
        - $ref: '#/components/parameters/SessionId'
        - $ref: '#/components/parameters/UserId'
      responses:
        '200':
          description: User context retrieved successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/WhoAmIResponse'
        '401':
          description: Authentication required
          
  /api/ems/v1/caniuse:
    post:
      tags:
        - EMS
      summary: Check user authorization
      description: |
        Check if the current user can perform specific actions on resources.
        Returns detailed authorization decisions for frontend use.
      parameters:
        - $ref: '#/components/parameters/SessionId'
        - $ref: '#/components/parameters/UserId'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/EMSAuthRequest'
      responses:
        '200':
          description: Authorization check completed
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/EMSAuthResponse'
```

### 2. New Schema Definitions

**Schema Additions:**
```yaml
components:
  parameters:
    UserId:
      name: X-User-Id
      in: header
      required: false
      schema:
        type: string
        format: uuid
      description: Direct user ID for API Gateway integration

  schemas:
    WhoAmIResponse:
      type: object
      properties:
        success:
          type: boolean
        user:
          $ref: '#/components/schemas/UserContext'
        roles:
          type: array
          items:
            $ref: '#/components/schemas/RoleContext'
        departments:
          type: array
          items:
            $ref: '#/components/schemas/DepartmentContext'
        permissions:
          type: array
          items:
            $ref: '#/components/schemas/PermissionContext'
        context:
          $ref: '#/components/schemas/SessionContext'

    EMSAuthRequest:
      type: object
      properties:
        resourceId:
          type: string
          example: "CMS-10-20045"
        actionId:
          type: string
          example: "CREATE_CASE"
        resourceType:
          type: string
          example: "case"
        context:
          type: object
          additionalProperties: true

    EMSAuthResponse:
      type: object
      properties:
        success:
          type: boolean
        actions:
          type: array
          items:
            type: object
            properties:
              actionId:
                type: string
              displayName:
                type: string
              allowed:
                type: boolean
              reason:
                type: string
        resourceAccess:
          type: object
          properties:
            canRead:
              type: boolean
            canWrite:
              type: boolean
            canDelete:
              type: boolean
            canApprove:
              type: boolean
        derivedRoles:
          type: array
          items:
            type: string
        evaluationTime:
          type: number
```

## Integration Points

### 1. Existing Service Reuse

**UserService Integration:**
- `findById()` - User lookup
- `findByUsername()` - Session-based lookup
- User attribute retrieval

**HybridAuthorizationService Integration:**
- Policy evaluation via Cerbos
- Database-level permission checks
- Principal context building

**BusinessAppRoleService Integration:**
- User role assignments
- Role metadata and queue information
- Business application context

### 2. Session Management

**AuthController Integration:**
- Reuse existing session validation logic
- Session expiration handling
- Session info extraction

### 3. Caching Strategy

**Implementation Options:**
- Spring Cache abstraction
- User context caching (5-10 minutes)
- Permission matrix caching
- Cache invalidation on role changes

## Error Handling Strategy

### 1. Consistent Error Responses

**Format:**
```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Session expired or invalid",
    "timestamp": "2025-01-09T10:30:00Z",
    "path": "/api/ems/v1/whoami"
  }
}
```

### 2. HTTP Status Codes

**Mapping:**
- `200` - Success (both endpoints)
- `401` - Unauthorized (invalid session/user)
- `403` - Forbidden (valid user, no access)
- `400` - Bad Request (invalid request format)
- `500` - Internal Server Error

### 3. Validation

**Request Validation:**
- Optional field validation
- Resource ID format validation
- Action ID enumeration validation

## Testing Strategy

### 1. Unit Tests

**Test Classes:**
- `EMSControllerTest.java`
- `EMSServiceTest.java`
- DTO validation tests

### 2. Integration Tests

**Scenarios:**
- Session-based authentication
- Direct user ID authentication
- Role aggregation accuracy
- Permission matrix construction
- Cerbos integration

### 3. API Tests

**Postman Collection Updates:**
- Add EMS endpoint examples
- Authentication scenarios
- Error case validation

## Security Considerations

### 1. Authentication

**Security Measures:**
- Session validation reuse
- User ID validation
- Rate limiting consideration
- Input sanitization

### 2. Authorization

**Access Control:**
- User can only access own context
- No privilege escalation through API
- Audit logging for authorization checks

### 3. Data Exposure

**Information Disclosure:**
- Limit sensitive attribute exposure
- Role metadata filtering
- Department information scoping

## Performance Considerations

### 1. Caching

**Cache Implementation:**
```java
@Cacheable(value = "userContext", key = "#userId")
public WhoAmIResponse buildUserContext(String userId) {
    // Implementation
}

@Cacheable(value = "userPermissions", key = "#userId")  
public Map<String, List<String>> getUserPermissionMatrix(String userId) {
    // Implementation
}
```

### 2. Database Optimization

**Query Strategy:**
- Single query for user + roles + departments
- Lazy loading for non-critical data
- Index optimization for user lookups

### 3. Response Size

**Optimization:**
- Pagination for large role lists
- Optional fields in response
- Compressed JSON responses

## Implementation Timeline

### Phase 1 (Week 1): Core Structure
- [ ] Create EMSController skeleton
- [ ] Define DTO classes
- [ ] Basic endpoint routing
- [ ] Authentication integration

### Phase 2 (Week 2): Service Implementation  
- [ ] EMSService implementation
- [ ] User context aggregation
- [ ] Authorization check logic
- [ ] Error handling

### Phase 3 (Week 3): Integration & Testing
- [ ] Existing service integration
- [ ] Unit test implementation
- [ ] Integration testing
- [ ] API validation

### Phase 4 (Week 4): Documentation & Deployment
- [ ] OpenAPI specification updates
- [ ] Swagger UI validation
- [ ] Postman collection updates
- [ ] Performance testing

## Deployment Considerations

### 1. Backwards Compatibility
- No changes to existing endpoints
- New namespace isolation
- Independent versioning

### 2. Monitoring
- Endpoint usage metrics
- Response time monitoring  
- Error rate tracking
- Cache hit/miss ratios

### 3. Rollback Strategy
- Feature flag implementation
- Independent deployment capability
- Database schema independence

This implementation plan provides a comprehensive approach to adding the EMS endpoints while maintaining consistency with existing service patterns and ensuring robust frontend integration support.