# Entitlement Service - Hybrid Authorization System

## Overview

The Entitlement Service is a comprehensive authorization and user management microservice that implements a **hybrid authorization architecture**. This service provides flexible authorization capabilities through two distinct authorization engines: a **Database-based RBAC engine** and a **Cerbos-based ABAC engine**. The system allows runtime configuration to select the appropriate engine based on operational requirements.

## Architecture

### Hybrid Authorization Pattern

The service implements the **Adapter Pattern** to provide a unified authorization interface that can seamlessly switch between different authorization engines:

```
┌─────────────────────────────────────────────────────────────┐
│                    Client Applications                        │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│              API Gateway (Port 8080)                        │
│           Session Validation & Routing                      │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│            Entitlement Service (Port 8081)                  │
│                                                             │
│  ┌─────────────────────────────────────────────────────────┐│
│  │         HybridAuthorizationService                      ││
│  │              (Orchestrator)                             ││
│  └─────────────────────┬───────────────────────────────────┘│
│                        │                                    │
│  ┌─────────────────────▼───────────────────────────────────┐│
│  │         AuthorizationEngine Interface                   ││
│  │            (Adapter Pattern)                            ││
│  └─────────────┬───────────────────────┬───────────────────┘│
│                │                       │                    │
│  ┌─────────────▼──────────┐  ┌────────▼─────────────────────┐│
│  │ DatabaseAuthorizationEngine │  │ CerbosAuthorizationEngine ││
│  │        (RBAC)          │  │         (ABAC)              ││
│  │  - Role-based access   │  │  - Policy-based access      ││
│  │  - Permission mapping  │  │  - Attribute evaluation     ││
│  │  - Database queries    │  │  - External policy engine   ││
│  └────────────────────────┘  └──────────────────────────────┘│
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Core Components

#### 1. **HybridAuthorizationService** (Orchestrator)
- **Purpose**: Main entry point for all authorization decisions
- **Responsibility**: Routes requests to the appropriate authorization engine
- **Configuration**: Uses `authorization.engine.use-cerbos` flag to select engine
- **Features**:
  - Unified interface for authorization checks
  - Engine health monitoring
  - Error handling and fallback mechanisms
  - Audit logging support

#### 2. **AuthorizationEngine Interface** (Adapter Pattern)
- **Purpose**: Defines the contract for authorization engines
- **Benefits**: 
  - **Interchangeability**: Engines can be swapped without changing client code
  - **Testability**: Easy to mock and test different authorization scenarios
  - **Extensibility**: New engines can be added by implementing the interface
  - **Maintainability**: Engine-specific logic is encapsulated

#### 3. **DatabaseAuthorizationEngine** (RBAC Implementation)
- **Authorization Model**: Role-Based Access Control (RBAC)
- **Data Sources**: PostgreSQL database tables
- **Performance**: High-performance with caching layers
- **Use Case**: Traditional role-based permissions with database persistence

#### 4. **CerbosAuthorizationEngine** (ABAC Implementation)
- **Authorization Model**: Attribute-Based Access Control (ABAC)
- **External Engine**: Cerbos Policy Decision Point (PDP)
- **Policy Language**: Cedar-like policy definitions
- **Use Case**: Complex policy-based authorization with dynamic attributes

## Database Schema

### Hybrid Architecture Tables

The database schema is designed to support both authorization engines with optimized table structures:

#### Core Identity Tables
- **`entitlement_core_users`** - Primary user identity (UUID-based)
  - Global user attributes stored as JSONB
  - Used by both engines for user context

#### Cerbos Engine Tables
- **`business_applications`** - Application definitions
- **`business_app_roles`** - Roles within applications  
- **`user_business_app_roles`** - User-to-role assignments
- **`departments`** - Organizational departments
- **`user_departments`** - User-to-department assignments

#### Database Engine Tables  
- **`entitlement_application_domains`** - Multi-tenant domains
- **`entitlement_domain_roles`** - Roles within domains
- **`entitlement_permissions`** - Granular permissions
- **`entitlement_role_permissions`** - Role-to-permission mappings
- **`entitlement_user_domain_roles`** - User-to-role assignments
- **`resource_permissions`** - Resource-specific permissions

#### Shared Infrastructure
- **`entitlement_audit_logs`** - Authorization decision audit trail
  - Records decisions from both engines
  - Includes engine type, decision reasoning, and metadata

### Database Performance Features
- **Optimized Indexing**: GIN indexes for JSONB attributes
- **Connection Pooling**: HikariCP with leak detection  
- **Caching Strategy**: Caffeine cache for user context and permissions
- **Audit Trail**: Complete authorization decision logging

## Configuration Management

### Engine Selection Flag

The system uses a configuration-driven approach for engine selection:

```yaml
authorization:
  engine:
    # Primary engine selector - DEFAULT: false (Database Engine)
    use-cerbos: ${AUTHORIZATION_USE_CERBOS:false}
    # Runtime switching capability (for testing)
    allow-runtime-switching: ${AUTHORIZATION_ALLOW_RUNTIME_SWITCHING:false}
```

#### Key Configuration Points:

1. **Default Engine**: Database engine (`use-cerbos: false`)
   - **Reason**: Provides reliable operation without external dependencies
   - **Use Case**: Production environments where Cerbos is not available
   - **Benefits**: Self-contained, high performance, proven reliability

2. **Cerbos Engine**: Policy-based engine (`use-cerbos: true`)
   - **Requirement**: Cerbos server must be available and configured
   - **Use Case**: Complex authorization scenarios requiring policy evaluation
   - **Benefits**: Advanced ABAC capabilities, external policy management

3. **Runtime Configuration**: Environment variables take precedence
   - `AUTHORIZATION_USE_CERBOS=true` enables Cerbos engine
   - `AUTHORIZATION_USE_CERBOS=false` enables Database engine

### Cerbos Integration Configuration

```yaml
cerbos:
  host: ${CERBOS_HOST:localhost}
  port: ${CERBOS_PORT:3592}
  tls:
    enabled: ${CERBOS_TLS_ENABLED:false}
  policies:
    path: classpath:cerbos/policies
    auto-load: ${CERBOS_AUTO_LOAD_POLICIES:true}
    validate-on-startup: ${CERBOS_VALIDATE_POLICIES:true}
```

## Policy Management

### Policy Loading via API Endpoints

The service provides REST endpoints for policy management and validation:

#### 1. **Policy Status Endpoint**
```http
GET /api/policies/status
```
- **Purpose**: Check Cerbos connection and policy inventory
- **Response**: Connection status and policy validation results
- **Use Case**: Health monitoring and troubleshooting

#### 2. **Policy Validation Endpoint**
```http
POST /api/policies/validate
```
- **Purpose**: Validate all policy files against Cerbos schema
- **Response**: Validation results and error details
- **Use Case**: Deployment verification and policy testing

#### 3. **Policy Content Endpoint**
```http
GET /api/policies/content/{policyType}/{fileName}
```
- **Purpose**: Retrieve specific policy file content
- **Parameters**: 
  - `policyType`: resources, derived_roles, principal
  - `fileName`: Policy file name (e.g., case-nextgen.yaml)
- **Use Case**: Policy debugging and content verification

### Policy Structure
```
src/main/resources/cerbos/policies/
├── derived_roles/        # Dynamic role definitions
│   └── one-cms.yaml
├── principal/           # User base policies  
│   └── user-base.yaml
└── resources/          # Resource-specific policies
    ├── case-nextgen.yaml
    ├── case.yaml
    ├── one-cms-workflow.yaml
    └── workflow-nextgen.yaml
```

## Importance of Adapter Pattern

### Benefits of the Adapter Pattern Implementation

#### 1. **Flexibility and Interchangeability**
- **Runtime Engine Switching**: Change authorization engines without service restart
- **Configuration-Driven**: Simple property changes control engine selection
- **Zero-Downtime Migration**: Seamless transition between authorization models

#### 2. **Risk Mitigation**
- **Fallback Capability**: Database engine provides reliable fallback when Cerbos is unavailable
- **Dependency Isolation**: External Cerbos dependency doesn't impact core functionality
- **Operational Resilience**: System remains functional regardless of external service status

#### 3. **Development and Testing Benefits**
- **Independent Testing**: Each engine can be tested in isolation
- **Mock Implementation**: Easy to create test doubles for authorization logic
- **Integration Testing**: Test both engines with the same test suite

#### 4. **Maintenance and Evolution**
- **Separation of Concerns**: Engine-specific logic is encapsulated
- **Code Reusability**: Common authorization patterns shared across engines
- **Future Extensions**: Easy to add new authorization engines (e.g., AWS IAM, Azure AD)

#### 5. **Performance Optimization**
- **Engine-Specific Optimizations**: Each engine can optimize for its strengths
- **Caching Strategy**: Different caching approaches for different engines
- **Load Distribution**: Route different types of requests to optimal engines

## System Integration

### Service Communication
- **Port**: 8081
- **Protocol**: HTTP/REST
- **Authentication**: Session-based via API Gateway
- **Service Discovery**: Eureka registration

### Core API Endpoints
- `POST /api/entitlements/check` - Primary authorization endpoint
- `GET /api/entitlements/health` - Service health check
- `GET /api/policies/*` - Policy management endpoints

### Performance Characteristics
- **Cache Performance**: Sub-millisecond response for cached decisions
- **Database Performance**: Single-digit millisecond response for uncached decisions
- **Cerbos Performance**: Policy evaluation typically under 50ms
- **Throughput**: Supports thousands of concurrent authorization checks

### Monitoring and Observability
- **Metrics**: Prometheus integration for performance monitoring
- **Health Checks**: Actuator endpoints for operational monitoring
- **Audit Trail**: Complete decision logging for compliance and debugging
- **Cache Statistics**: Detailed cache hit/miss ratios and performance metrics

## Operational Considerations

### Production Deployment
1. **Default Configuration**: Start with database engine for stability
2. **Cerbos Migration**: Gradually enable Cerbos engine with proper testing
3. **Policy Validation**: Always validate policies before deployment
4. **Health Monitoring**: Monitor both engine types and their dependencies

### Troubleshooting
- **Engine Health**: Use `/api/policies/status` to check Cerbos connectivity
- **Policy Issues**: Use validation endpoints to verify policy correctness
- **Performance**: Monitor cache statistics and query performance
- **Audit**: Review audit logs for authorization decision patterns

The hybrid authorization system provides enterprise-grade authorization capabilities with the flexibility to choose the right engine for specific operational requirements while maintaining system reliability and performance.