# Hybrid Authorization System - Source of Truth
**Version**: 2.0.0  
**Last Updated**: 2025-08-28  
**Project**: NextGen Workflow Entitlement Service  

## 🎯 BUSINESS REQUIREMENTS (PRIMARY SOURCE OF TRUTH)

### Core Identity Management
- **User Identity**: All users MUST be identified by UUID (never String/Long)
- **Single User Source**: `entitlement_core_users` is the ONLY user identity table
- **Username Uniqueness**: Usernames must be unique across the entire system
- **Email Uniqueness**: Email addresses must be unique across the entire system
- **User Lifecycle**: Users can be deactivated but never deleted (soft delete)

### Authorization Engine Requirements
- **Dual Engine Support**: System MUST support both Database RBAC and Cerbos ABAC
- **Engine Switching**: `authorization.engine.use-cerbos` flag controls which engine is active
- **Transparent Operation**: Services must work identically regardless of active engine
- **Fallback Strategy**: If Cerbos fails, system should gracefully handle fallback (no automatic switch)

### Authorization Models

#### Database Engine (RBAC)
- **Domain-Based**: Multi-tenant support via application domains
- **Role Hierarchy**: Roles can have different levels (Tier1, Tier2, Manager, Admin)
- **Permission Mapping**: Roles map to specific resource-action permissions
- **Resource-Level Access**: Direct user-to-resource permission grants for ABAC-style access

#### Cerbos Engine (ABAC)  
- **Business Application Roles**: Users assigned roles within specific business applications
- **Department-Based Access**: User department membership affects access decisions
- **Policy-Driven**: Access decisions based on Cerbos policies (case.yaml, workflow.yaml)
- **Dynamic Attributes**: User attributes and resource context drive decisions

### Data Consistency Requirements
- **ACID Compliance**: All database operations must be transactional
- **Audit Trail**: Every authorization decision must be logged
- **Data Integrity**: Foreign key constraints must be enforced
- **Performance**: Authorization decisions must complete within 500ms

## 🏛️ TECHNICAL ARCHITECTURE (DERIVED FROM REQUIREMENTS)

### Database Architecture

#### Core Schema Design
```sql
-- Primary user identity (UUID-based)
entitlements.entitlement_core_users (user_id UUID PRIMARY KEY)

-- Shared reference data
entitlements.departments (id BIGSERIAL)
entitlements.business_applications (id BIGSERIAL)  
entitlements.business_app_roles (id BIGSERIAL)

-- Database engine tables (RBAC)
entitlements.entitlement_application_domains (domain_id UUID)
entitlements.entitlement_domain_roles (role_id UUID)
entitlements.entitlement_permissions (permission_id UUID)
entitlements.entitlement_role_permissions (role_permission_id UUID)
entitlements.entitlement_user_domain_roles (user_role_id UUID)

-- Resource-level permissions (ABAC-style within database)
entitlements.resource_permissions (resource_permission_id UUID)

-- Cerbos engine tables (ABAC)  
entitlements.user_departments (id BIGSERIAL)
entitlements.user_business_app_roles (id BIGSERIAL)

-- Audit and monitoring
entitlements.entitlement_audit_logs (audit_id UUID)
```

#### Database Naming Conventions (ENFORCED)
- **Tables**: Plural, snake_case (e.g., `entitlement_core_users`)
- **Primary Keys**: `{entity}_id` (e.g., `user_id`, `role_id`, `domain_id`)
- **Foreign Keys**: `{referenced_entity}_id` (e.g., `user_id`, `department_id`)
- **Booleans**: `is_` or `has_` prefix (e.g., `is_active`, `has_access`)
- **Timestamps**: `{action}_at` (e.g., `created_at`, `updated_at`, `expires_at`)
- **Junction Tables**: Both entity names (e.g., `user_departments`, `role_permissions`)

### Service Architecture

#### Authorization Engine Interface
```java
public interface AuthorizationEngine {
    AuthorizationDecision checkAuthorization(AuthorizationRequest request);
    Principal buildPrincipal(String userId);
    boolean isEngineHealthy();
}
```

#### Engine Implementations
- **DatabaseAuthorizationEngine**: Implements RBAC using database queries
- **CerbosAuthorizationEngine**: Implements ABAC using Cerbos policy engine
- **HybridAuthorizationService**: Orchestrates engine selection and fallback

#### Service Layer Requirements
- **Interface Segregation**: Separate interfaces for different functional areas
- **Implementation Pattern**: All service interfaces must have concrete `@Service` implementations
- **Transaction Management**: Use `@Transactional` for all data-modifying operations
- **Error Handling**: Consistent exception handling with specific exception types
- **Circuit Breaker**: External service calls must use circuit breaker pattern

### API Architecture

#### REST API Standards
- **Base Path**: `/api/entitlements/v1`
- **Resource Naming**: Plural nouns (e.g., `/users`, `/roles`, `/permissions`)
- **HTTP Methods**: 
  - `GET` for retrieval (list and single)
  - `POST` for creation
  - `PUT` for full updates
  - `PATCH` for partial updates  
  - `DELETE` for removal
- **Status Codes**: Use appropriate HTTP status codes (200, 201, 400, 401, 403, 404, 500)

#### Request/Response Format
- **Content Type**: `application/json` for all requests and responses
- **User IDs**: Always UUID format in JSON (e.g., `"550e8400-e29b-41d4-a716-446655440001"`)
- **Timestamps**: ISO 8601 format with timezone (e.g., `"2025-08-28T10:00:00Z"`)
- **Pagination**: Use `page`, `size`, `sort` parameters for list endpoints
- **Error Format**: Consistent error response structure

#### Authentication & Authorization
- **Authentication**: Session-based with `X-Session-Id` header
- **User Context**: API Gateway adds `X-User-Id` header (UUID format)
- **Authorization**: Each endpoint validates permissions via authorization engine

## 📊 IMPLEMENTATION CONTRACTS (DERIVED FROM ARCHITECTURE)

### DTO Standards

#### Naming Conventions
- **Entity DTOs**: `{Entity}Dto` (e.g., `UserDto`, `RoleDto`)
- **Request DTOs**: `{Operation}{Entity}Request` (e.g., `CreateUserRequest`)
- **Response DTOs**: `{Operation}{Entity}Response` (e.g., `CreateUserResponse`)
- **Search DTOs**: `{Entity}SearchCriteria` (e.g., `UserSearchCriteria`)

#### Property Standards
```java
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    private UUID userId;                    // Always UUID, never String
    private String username;                // Always non-null for active users
    private String email;                   // Always non-null and validated
    private String firstName;               // Always non-null
    private String lastName;                // Always non-null  
    private Boolean isActive;               // Always non-null
    private JsonNode globalAttributes;      // JSONB attributes from database
    private Instant createdAt;              // ISO 8601 timestamp
    private Instant updatedAt;              // ISO 8601 timestamp
}
```

#### Validation Requirements
- **Input Validation**: All request DTOs must have Bean Validation annotations
- **UUID Validation**: All UUID fields must validate format
- **Email Validation**: Email fields must use `@Email` annotation
- **Size Constraints**: String fields must have `@Size` constraints matching database

### Service Interface Standards

#### Method Naming Conventions
```java
// Retrieval methods
Optional<UserDto> findUserById(UUID userId);
Page<UserDto> findUsers(UserSearchCriteria criteria, Pageable pageable);
List<UserDto> findActiveUsers();

// Creation methods  
UserDto createUser(CreateUserRequest request);
List<UserDto> createUsers(List<CreateUserRequest> requests);

// Update methods
UserDto updateUser(UUID userId, UpdateUserRequest request);
UserDto patchUser(UUID userId, PatchUserRequest request);

// Deletion methods
void deleteUser(UUID userId);              // Soft delete
void deactivateUser(UUID userId);         // Explicit deactivation

// Authorization methods
AuthorizationDecision checkUserAuthorization(UUID userId, String resource, String action);
```

#### Exception Standards
```java
// Specific exceptions for different error cases
public class UserNotFoundException extends EntityNotFoundException { }
public class UserAlreadyExistsException extends EntityAlreadyExistsException { }
public class InvalidUserDataException extends ValidationException { }
public class AuthorizationFailedException extends SecurityException { }
```

### Repository Standards

#### JPA Entity Standards
```java
@Entity
@Table(name = "entitlement_core_users", schema = "entitlements")
public class EntitlementCoreUser {
    @Id
    @Column(name = "user_id")
    private UUID userId;
    
    @Column(name = "username", nullable = false, unique = true)
    private String username;
    
    // Other fields...
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @UpdateTimestamp  
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
```

#### Repository Method Standards
```java
public interface EntitlementCoreUserRepository extends JpaRepository<EntitlementCoreUser, UUID> {
    Optional<EntitlementCoreUser> findByUsername(String username);
    Optional<EntitlementCoreUser> findByEmail(String email);
    List<EntitlementCoreUser> findByIsActiveTrue();
    
    @Query("SELECT u FROM EntitlementCoreUser u WHERE u.globalAttributes ->> 'department' = :department")
    List<EntitlementCoreUser> findByDepartment(@Param("department") String department);
}
```

## 🔄 SYNCHRONIZATION REQUIREMENTS

### Document Update Protocol
1. **Requirements Change**: Update Business Requirements section first
2. **Architecture Impact**: Update Technical Architecture to reflect requirements  
3. **Implementation Update**: Update Implementation Contracts to match architecture
4. **Code Generation**: Regenerate OpenAPI spec from updated contracts
5. **Implementation**: Update Java code to match contracts

### Validation Gates (MANDATORY)

#### After Requirements Change
```bash
# Validate document consistency
grep -n "UUID\|String" HYBRID_SYSTEM_SOURCE_OF_TRUTH.md
# Ensure no contradictions between sections
```

#### After Architecture Update  
```bash
# Validate OpenAPI alignment
swagger-codegen validate -i entitlement-service-openapi.yml
# Check database schema alignment
psql -d nextgen_workflow -c "\d+ entitlements.entitlement_core_users"
```

#### After Implementation Change
```bash
# MANDATORY validation sequence
mvn clean compile                    # Must pass
mvn spring-boot:run -Dspring.profiles.active=test  # Must start successfully
curl localhost:8081/actuator/health  # Must return 200
```

#### Before Commit
```bash
# Full validation suite
mvn clean test                       # All tests must pass
mvn spring-boot:run                  # Service must start
# Manual API testing with Postman    # Critical endpoints must work
```

## 📋 CURRENT STATE TRACKING

### ✅ COMPLETED COMPONENTS

#### Database Schema: FULLY ALIGNED
- **Status**: ✅ Complete
- **Details**: All 13 hybrid tables created with proper UUID primary keys
- **Validation**: Schema matches Hybrid_Approach.md specifications exactly
- **Migration Files**: Clean 2-file Liquibase migration (001-hybrid-schema.xml, 002-sample-data.xml)

#### Configuration System: FULLY ALIGNED  
- **Status**: ✅ Complete
- **Details**: Engine switching via `authorization.engine.use-cerbos` flag
- **Validation**: HybridAuthorizationConfig properly configured

### ❌ COMPONENTS REQUIRING IMPLEMENTATION

#### Service Layer: CRITICAL GAP
- **Status**: ❌ Interface-only (no implementations)
- **Impact**: Application startup will fail
- **Required**: Implement all service interfaces with `@Service` annotations
- **Priority**: HIGH (blocks all functionality)

#### DTO Layer: CRITICAL ALIGNMENT ISSUES
- **Status**: ❌ Property mismatches and ID type inconsistencies  
- **Impact**: Compilation failures in controllers
- **Required**: Fix all DTOs to use UUID types and add missing properties
- **Priority**: HIGH (blocks compilation)

#### Controller Layer: COMPILATION FAILURES
- **Status**: ❌ Calling non-existent service methods
- **Impact**: mvn compile fails
- **Required**: Align controller calls with actual service interface methods
- **Priority**: HIGH (blocks development)

#### OpenAPI Specification: OUT OF SYNC
- **Status**: ❌ Uses String user IDs, missing endpoints
- **Impact**: API documentation misleading, client generation fails
- **Required**: Regenerate spec to match current implementation contracts
- **Priority**: MEDIUM (documentation/tooling impact)

## 🚨 BREAKING CHANGE POLICY

### Items That Can NEVER Change (Breaking Changes)
- **User ID Type**: UUID (never String/Long)
- **Core User Table**: `entitlement_core_users` as single identity source
- **Database Schema**: `entitlements` schema name
- **Engine Interface**: `AuthorizationEngine.checkAuthorization()` method signature
- **Authentication**: Session-based with `X-Session-Id` header

### Items That Require Cross-Component Updates
- **DTO Property Names**: Must update controllers, services, and OpenAPI
- **Service Method Signatures**: Must update controllers and interface implementations
- **Database Column Names**: Must update entities, repositories, and migration scripts
- **API Endpoint Paths**: Must update controllers, OpenAPI, and client documentation

### Change Approval Process
1. **Document Impact Analysis**: Update this source of truth document
2. **Technical Review**: Validate against architecture principles
3. **Implementation Planning**: Create detailed task breakdown
4. **Validation Strategy**: Define success criteria and testing approach
5. **Rollback Plan**: Define how to revert if issues arise

## 📊 SUCCESS METRICS

### Implementation Success Criteria
- **Compilation**: `mvn clean compile` succeeds without errors
- **Service Startup**: `mvn spring-boot:run` starts service successfully  
- **Health Check**: `GET /actuator/health` returns 200 OK
- **Core Functionality**: User CRUD operations work via REST API
- **Engine Switching**: Both database and Cerbos engines function properly
- **Authorization Decisions**: All test scenarios return expected allow/deny decisions

### Quality Gates  
- **Code Coverage**: Minimum 80% test coverage for service layer
- **Performance**: Authorization decisions complete within 500ms
- **Error Handling**: All error scenarios return appropriate HTTP status codes
- **Documentation**: OpenAPI spec 100% accurate with current implementation
- **Data Integrity**: All database constraints enforced, no orphaned records

---

**Document Ownership**: Development Team  
**Review Frequency**: Updated before any major implementation change  
**Approval Required**: Yes, for any breaking changes to core contracts