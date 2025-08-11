# NextGen Workflow Microservices Architecture

## Table of Contents
1. [Overall Architecture](#overall-architecture)
2. [Service Registry](#service-registry)
3. [API Gateway](#api-gateway)
4. [Entitlement Service](#entitlement-service)
5. [Core Workflow Engine](#core-workflow-engine)
6. [OneCMS Service](#onecms-service)
7. [React UI Application](#react-ui-application)
8. [Database Design](#database-design)
9. [API Specifications](#api-specifications)
10. [BPMN Workflow Processes](#bpmn-workflow-processes)
11. [Cerbos Policy Integration](#cerbos-policy-integration)
12. [Deployment Architecture](#deployment-architecture)

## Overall Architecture

The NextGen Workflow system is built using a microservices architecture pattern with the following key components:

```
                        ┌─────────────────┐    
                        │   React UI App  │    
                        │   (Port 3000)   │ 
                        │                 │   
                        └─────────┬───────┘   
                                  │           
                                  │
                    ┌─────────────▼─────────────┐
                    │      API Gateway          │
                    │      (Port 8080)          │
                    └─────────────┬─────────────┘
                                  │
                    ┌─────────────▼─────────────┐
                    │   Service Registry        │
                    │   (Eureka - Port 8761)    │
                    └─────────────┬─────────────┘
                                  │
        ┌─────────────────────────┼─────────────────────────┐
        │                         │                         │
┌───────▼────────┐    ┌──────────▼──────────┐    ┌─────────▼────────┐
│ Entitlement    │    │ Core Workflow       │    │ OneCMS Service   │
│ Service        │    │ Engine              │    │ (Port 8083)      │
│ (Port 8081)    │    │ (Port 8082)         │    │                  │
│  + Cerbos      │    │  + Flowable 7.1.0   │    │                  │
└───────┬────────┘    └──────────┬──────────┘    └─────────┬────────┘
        │                        │                         │
┌───────▼────────┐    ┌──────────▼──────────┐    ┌─────────▼────────┐
│ Entitlements   │    │ Flowable Database   │    │ OneCMS Database  │
│ Database       │    │ (Schema: flowable)  │    │ (Schema: onecms) │
│ (PostgreSQL)   │    │ (PostgreSQL)        │    │ (PostgreSQL)     │
└────────────────┘    └─────────────────────┘    └──────────────────┘
```

### Key Architectural Principles

1. **Microservices Pattern**: Each service has a single responsibility
2. **Database per Service**: Each service owns its data
3. **API Gateway Pattern**: Single entry point for all client requests with session-based authentication
4. **Service Discovery**: Eureka-based service registration and discovery
5. **Event-Driven Architecture**: Asynchronous communication between services
6. **CQRS**: Command Query Responsibility Segregation for complex operations
7. **Circuit Breaker**: Resilience patterns for service communication
8. **Policy-Driven Authorization**: Cerbos-based fine-grained access control
9. **Session-Based Security**: Simplified authentication using session IDs and header propagation

### Communication Patterns

```
Frontend (React) 
    ↓ HTTP/REST
API Gateway (8080)
    ↓ Load Balanced Routing
┌─────────────────────────┬─────────────────────────┬─────────────────────────┐
│                         │                         │                         │
Entitlement Service     Workflow Engine         OneCMS Service
(8081)                 (8082)                  (8083)
    ↓                       ↓                       ↓
Cerbos Policy Engine   Flowable BPMN Engine    Business Logic
    ↓                       ↓                       ↓
PostgreSQL             PostgreSQL              PostgreSQL
(entitlements)        (flowable)              (onecms)
```

## Service Registry

### Overview
The Service Registry is built using Netflix Eureka Server and serves as the central registry for all microservices in the ecosystem.

### High-Level Design
- **Technology**: Spring Cloud Netflix Eureka Server
- **Port**: 8761
- **Purpose**: Service discovery and registration
- **Dependencies**: None (standalone service)

### Low-Level Design

#### Key Components
1. **EurekaServer**: Main server component
2. **Service Registry Dashboard**: Web UI for monitoring registered services
3. **Health Check Mechanism**: Monitors service health with 30-second intervals
4. **Load Balancing**: Provides service instances for client-side load balancing

#### Configuration
```yaml
server:
  port: 8761

eureka:
  instance:
    hostname: localhost
  client:
    register-with-eureka: false
    fetch-registry: false
    service-url:
      defaultZone: http://localhost:8761/eureka
  server:
    enable-self-preservation: false
    eviction-interval-timer-in-ms: 10000
```

#### Features
- Service registration and deregistration
- Health monitoring with configurable intervals
- Load balancing support for service clients
- Failover capabilities
- Dashboard for service monitoring
- Service metadata management

### REST Endpoints
- `GET /` - Eureka Dashboard
- `GET /eureka/apps` - List all registered applications
- `GET /eureka/apps/{appName}` - Get specific application details
- `POST /eureka/apps/{appName}` - Register application instance
- `DELETE /eureka/apps/{appName}/{instanceId}` - Deregister instance
- `PUT /eureka/apps/{appName}/{instanceId}/status` - Update instance status

## API Gateway

### Overview
The API Gateway serves as the single entry point for all client requests, handling routing, security, rate limiting, and cross-cutting concerns.

### High-Level Design
- **Technology**: Spring Cloud Gateway with Netty
- **Port**: 8080
- **Purpose**: Request routing, security, and API management
- **Dependencies**: Service Registry (Eureka)

### Low-Level Design

#### Key Components
1. **Gateway Routes**: Define routing rules with predicates and filters
2. **Load Balancer**: Distributes requests across service instances
3. **Circuit Breaker**: Handles service failures gracefully
4. **Rate Limiting**: Controls request rates per client
5. **CORS Handler**: Manages cross-origin resource sharing
6. **Security Filters**: Session ID validation and authentication

#### Route Configuration

The API Gateway includes a custom filter for session-based authentication that validates session IDs and injects user information into downstream service requests.
```yaml
spring:
  cloud:
    gateway:
      # Enable service discovery integration
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true

      # Global Filters (Applied to all routes)
      default-filters:
        - RemoveResponseHeader=X-Powered-By
        - name: Retry
          args:
            retries: 2
            statuses: BAD_GATEWAY,SERVICE_UNAVAILABLE
            methods: GET,POST

      # Route Definitions
      routes:
        # Route for Entitlement Service
        - id: entitlement-service-route
          uri: lb://entitlement-service
          predicates:
            - Path=/api/entitlements/**
          filters:
            - name: Retry
              args:
                retries: 3
                statuses: BAD_GATEWAY,SERVICE_UNAVAILABLE
                methods: GET,POST,PUT,DELETE

        # Route for Flowable Core Workflow Service
        - id: flowable-core-workflow-route
          uri: lb://flowable-core-workflow
          predicates:
            - Path=/api/workflow/**
          filters:
            - name: Retry
              args:
                retries: 3
                statuses: BAD_GATEWAY,SERVICE_UNAVAILABLE
                methods: GET,POST,PUT,DELETE

        # Route for OneCMS Service (with context path handling)
        - id: onecms-service-route
          uri: lb://onecms-service
          predicates:
            - Path=/api/cms/**
          filters:
            - RewritePath=/api/cms/(?<segment>.*), /api/$\{segment}
            - name: Retry
              args:
                retries: 3
                statuses: BAD_GATEWAY,SERVICE_UNAVAILABLE
                methods: GET,POST,PUT,DELETE
```

#### Features
- Dynamic routing based on service discovery
- Load balancing with multiple algorithms (round-robin, weighted, etc.)
- Request/response transformation
- Rate limiting and throttling (Redis-backed)
- CORS handling for web applications
- Request logging and monitoring
- Circuit breaker integration with Hystrix fallbacks
- Session ID validation and user header injection

### Security Configuration

The API Gateway uses a custom `HeaderUserExtractionFilter` that:
1. Validates session IDs with the Entitlement Service
2. Extracts user information from the session
3. Injects `X-User-Id` headers into downstream requests
4. Handles session expiration and cleanup

```java
@Component
public class HeaderUserExtractionFilter implements GatewayFilter {
    // Validates X-Session-Id headers and adds X-User-Id for downstream services
}
```

### REST Endpoints
All endpoints are proxied through the gateway:
- `/api/entitlements/**` → Entitlement Service
- `/api/workflow/**` → Core Workflow Engine  
- `/api/cms/**` → OneCMS Service
- `/actuator/**` → Gateway health and metrics

## Entitlement Service

### Overview
The Entitlement Service manages user authentication, authorization, and role-based access control using Cerbos policy engine for fine-grained permissions.

### High-Level Design
- **Technology**: Spring Boot 3.3.4, Spring Security 6, Cerbos SDK
- **Port**: 8081
- **Database**: PostgreSQL (Schema: entitlements)
- **Purpose**: User management, role-based access control, policy enforcement

### Low-Level Design

#### Key Components
1. **User Management**: CRUD operations for users with attributes
2. **Business Application Management**: Multi-tenant application support
3. **Role Management**: Hierarchical role system with metadata
4. **Policy Engine Integration**: Cerbos-based authorization with YAML policies
5. **Session Management**: Session-based authentication with UUID session IDs

#### Architecture Layers
```
┌─────────────────────────────────────┐
│           Controllers               │
│  UserController, AuthController     │
│  BusinessAppController, etc.        │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│            Services                 │
│  UserService, AuthService           │
│  CerbosAuthorizationService         │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│      Cerbos Policy Engine           │
│    (Embedded/Sidecar Mode)          │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│          Repositories               │
│     JPA Repositories                │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│          Database                   │
│    PostgreSQL (entitlements)        │
└─────────────────────────────────────┘
```

#### Cerbos Policy Integration
- **Policy Definition**: YAML-based policy files for fine-grained access control
- **Resource Types**: Users, Cases, Workflows, Tasks, Departments
- **Actions**: CREATE, READ, UPDATE, DELETE, EXECUTE, CLAIM, COMPLETE
- **Conditions**: Attribute-based access control (ABAC)
- **Derived Roles**: Dynamic role assignment based on attributes

### Database Schema (Entitlements)

#### ER Diagram
```
┌─────────────────┐    ┌─────────────────────┐    ┌──────────────────────┐
│     users       │    │ business_applications│   │ business_app_roles   │
├─────────────────┤    ├─────────────────────┤    ├──────────────────────┤
│ id (PK)         │    │ id (PK)             │    │ id (PK)              │
│ username        │    │ business_app_name   │    │ business_app_id (FK) │
│ email           │    │ description         │    │ role_name            │
│ first_name      │    │ is_active           │    │ role_display_name    │
│ last_name       │    │ created_at          │    │ description          │
│ is_active       │    │ updated_at          │    │ is_active            │
│ created_at      │    │ metadata            │    │ created_at           │
│ updated_at      │    └─────────────────────┘    │ updated_at           │
│ attributes      │                               │ metadata             │
└─────────────────┘                               └──────────────────────┘
         │                                                 │
         │                                                 │
         └─────────────────┐                   ┌───────────┘
                           │                   │
                    ┌──────▼───────────────────▼──┐
                    │ user_business_app_roles     │
                    ├─────────────────────────────┤
                    │ id (PK)                     │
                    │ user_id (FK)                │
                    │ business_app_role_id (FK)   │
                    │ is_active                   │
                    │ granted_by                  │
                    │ granted_at                  │
                    │ expires_at                  │
                    │ metadata                    │
                    └─────────────────────────────┘
```

#### Tables Detail
1. **users**
   - id (VARCHAR(50), PK)
   - username (VARCHAR(100), UNIQUE)
   - email (VARCHAR(255), UNIQUE)
   - first_name (VARCHAR(100))
   - last_name (VARCHAR(100))
   - is_active (BOOLEAN DEFAULT TRUE)
   - created_at (TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
   - updated_at (TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
   - attributes (JSONB) - Stores user metadata like department, location, etc.

2. **business_applications**
   - id (BIGSERIAL, PK)
   - business_app_name (VARCHAR(100), UNIQUE)
   - description (TEXT)
   - is_active (BOOLEAN DEFAULT TRUE)
   - created_at (TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
   - updated_at (TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
   - metadata (JSONB) - Configuration data for the application

3. **business_app_roles**
   - id (BIGSERIAL, PK)
   - business_app_id (BIGINT, FK REFERENCES business_applications(id))
   - role_name (VARCHAR(100))
   - role_display_name (VARCHAR(255))
   - description (TEXT)
   - is_active (BOOLEAN DEFAULT TRUE)
   - created_at (TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
   - updated_at (TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
   - metadata (JSONB) - Role permissions and constraints

4. **user_business_app_roles**
   - id (BIGSERIAL, PK)
   - user_id (VARCHAR(50), FK REFERENCES users(id))
   - business_app_role_id (BIGINT, FK REFERENCES business_app_roles(id))
   - is_active (BOOLEAN DEFAULT TRUE)
   - granted_by (VARCHAR(50))
   - granted_at (TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
   - expires_at (TIMESTAMP)
   - metadata (JSONB) - Assignment-specific data

### REST Endpoints

#### Authentication
- `POST /api/auth/login` - User login (returns sessionId instead of JWT)
- `GET /api/auth/validate-session` - Validate session ID
- `GET /api/auth/user` - Get current user info from session
- `POST /api/auth/logout` - Logout and invalidate session

#### User Management
- `GET /api/users` - List all users (paginated)
- `GET /api/users/{id}` - Get user by ID
- `POST /api/users` - Create new user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user (soft delete)
- `GET /api/users/{id}/roles` - Get user's roles
- `POST /api/users/{id}/roles` - Assign role to user

#### Business Application Management
- `GET /api/business-applications` - List applications
- `POST /api/business-applications` - Create application
- `PUT /api/business-applications/{id}` - Update application
- `DELETE /api/business-applications/{id}` - Delete application

#### Role Management
- `GET /api/business-app-roles` - List roles for application
- `POST /api/business-app-roles` - Create role
- `PUT /api/business-app-roles/{id}` - Update role
- `DELETE /api/business-app-roles/{id}` - Delete role

#### Authorization
- `POST /api/entitlements/check` - Check user permissions
- `GET /api/entitlements/user/{userId}/permissions` - Get user permissions
- `POST /api/entitlements/bulk-check` - Check multiple permissions

## Core Workflow Engine

### Overview
The Core Workflow Engine is built on Flowable BPMN 7.1.0 engine and provides workflow orchestration capabilities with queue-based task management and event-driven architecture.

### High-Level Design
- **Technology**: Spring Boot 3.3.4, Flowable 7.1.0, PostgreSQL
- **Port**: 8082
- **Database**: PostgreSQL (Schema: flowable)
- **Purpose**: Workflow execution, task management, process orchestration

### Low-Level Design

#### Key Components
1. **Workflow Metadata Management**: BPMN process definitions and queue mappings
2. **Process Instance Management**: Workflow execution lifecycle
3. **Task Management**: Queue-based task distribution and assignment
4. **Event Listeners**: Real-time workflow event processing
5. **Queue Management**: Task routing based on business rules
6. **DMN Decision Engine**: Business rule evaluation

#### Architecture Layers
```
┌─────────────────────────────────────┐
│           Controllers               │
│ ProcessInstanceController           │
│ TaskController, MetadataController  │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│            Services                 │
│ ProcessInstanceService              │
│ TaskService, QueueTaskService       │
│ WorkflowMetadataService            │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│         Flowable Engine             │
│  RuntimeService, TaskService        │
│  HistoryService, RepositoryService  │
│  DmnEngine, FormService            │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│        Event Listeners              │
│  TaskCreatedListener               │
│  TaskCompletedListener             │
│  ProcessStartedListener            │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│          Database                   │
│    PostgreSQL (flowable)            │
│ Custom + Flowable Tables            │
└─────────────────────────────────────┘
```

#### BPMN Workflow Process
1. **Process Definition**: BPMN 2.0 XML files with task queue mappings
2. **Deployment**: Processes deployed to Flowable engine with validation
3. **Instantiation**: Process instances created with business keys and variables
4. **Task Creation**: User tasks created and automatically routed to queues
5. **Task Execution**: Tasks claimed, executed, and completed through queue system
6. **Process Completion**: Workflow reaches end state with audit trail

#### Workflow Metadata System
```yaml
# Example Workflow Metadata
processDefinitionKey: "case-management-process"
processName: "Case Management Workflow"
businessAppName: "onecms"
taskQueueMappings:
  - taskId: "initial-review"
    queue: "intake-queue"
    candidateGroups: ["INTAKE_ANALYST"]
  - taskId: "investigation"
    queue: "investigation-queue"
    candidateGroups: ["INVESTIGATOR", "SENIOR_INVESTIGATOR"]
  - taskId: "final-review"
    queue: "management-queue"
    candidateGroups: ["MANAGER"]
candidateGroupMappings:
  INTAKE_ANALYST: "intake-queue"
  INVESTIGATOR: "investigation-queue"
  MANAGER: "management-queue"
```

#### Workflow Registration API

The workflow service provides comprehensive APIs for registering and deploying BPMN workflows through the API Gateway.

##### Registration Process
1. **Metadata Registration**: Register workflow metadata with queue mappings
2. **BPMN Deployment**: Deploy BPMN XML to Flowable engine
3. **Validation**: Ensure process definition matches registered metadata
4. **Activation**: Make workflow available for process instantiation

##### API Endpoints (via API Gateway)

**Register Workflow Metadata**
```http
POST /api/workflow/{businessApp}/workflow-metadata/register
Content-Type: application/json
X-Session-Id: f89a178b-3214-4999-b261-db44726f5d44
X-User-Id: admin-user

{
  "processDefinitionKey": "investigationWorkflow",
  "processName": "Investigation Case Workflow",
  "businessAppName": "onecms",
  "description": "Standard workflow for investigation cases",
  "candidateGroupMappings": {
    "investigators": "investigation-queue",
    "managers": "management-queue",
    "legal": "legal-review-queue"
  },
  "metadata": {
    "version": "1.0",
    "category": "investigation",
    "priority": "high"
  }
}
```

**Deploy BPMN Workflow**
```http
POST /api/workflow/{businessApp}/workflow-metadata/deploy
Content-Type: application/json
X-Session-Id: f89a178b-3214-4999-b261-db44726f5d44
X-User-Id: admin-user

{
  "processDefinitionKey": "investigationWorkflow",
  "bpmnXml": "<?xml version=\"1.0\" encoding=\"UTF-8\"?>...",
  "deploymentName": "Investigation Workflow v1.0"
}
```

**Deploy from File**
```http
POST /api/workflow/{businessApp}/workflow-metadata/deploy-from-file?processDefinitionKey=investigationWorkflow&filename=investigation-workflow.bpmn
X-Session-Id: f89a178b-3214-4999-b261-db44726f5d44
X-User-Id: admin-user
```

**Get Workflow Metadata**
```http
GET /api/workflow/{businessApp}/workflow-metadata/{processDefinitionKey}
X-Session-Id: f89a178b-3214-4999-b261-db44726f5d44
```

##### Authorization Integration

All workflow registration operations require proper authorization through the Entitlement Service:
- **Register Action**: `workflow/{processDefinitionKey}` resource with `register` action
- **Deploy Action**: `workflow/{processDefinitionKey}` resource with `deploy` action  
- **View Action**: `workflow/{processDefinitionKey}` resource with `view` action

Security is enforced at the API level with automatic denial when authorization services are unavailable.

##### Example BPMN Process

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://www.flowable.org/processdef">
  
  <process id="investigationWorkflow" name="Investigation Case Workflow" isExecutable="true">
    
    <startEvent id="startEvent" name="Case Created" />
    
    <userTask id="reviewTask" name="Initial Review" flowable:candidateGroups="investigators">
      <documentation>Review the case for initial triage and assignment</documentation>
    </userTask>
    
    <userTask id="investigateTask" name="Investigation" flowable:candidateGroups="investigators">
      <documentation>Conduct detailed investigation of the case</documentation>
    </userTask>
    
    <userTask id="managerApproval" name="Manager Approval" flowable:candidateGroups="managers">
      <documentation>Manager review and approval of investigation findings</documentation>
    </userTask>
    
    <endEvent id="endEvent" name="Case Closed" />
    
    <!-- Sequence Flows -->
    <sequenceFlow id="flow1" sourceRef="startEvent" targetRef="reviewTask" />
    <sequenceFlow id="flow2" sourceRef="reviewTask" targetRef="investigateTask" />
    <sequenceFlow id="flow3" sourceRef="investigateTask" targetRef="managerApproval" />
    <sequenceFlow id="flow4" sourceRef="managerApproval" targetRef="endEvent" />
    
  </process>
  
</definitions>
```

#### Queue Management Architecture
- **Automatic Population**: Event listeners populate queue_tasks table
- **Priority-Based Ordering**: Tasks ordered by priority and creation time  
- **Assignment Support**: Both individual and group assignment
- **Status Tracking**: Real-time task status updates (OPEN, CLAIMED, COMPLETED)
- **Business Key Integration**: Links workflow tasks to business entities

### Database Schema (Flowable)

#### Custom Tables
1. **workflow_metadata**
   ```sql
   CREATE TABLE workflow_metadata (
       id BIGSERIAL PRIMARY KEY,
       process_definition_key VARCHAR(255) UNIQUE NOT NULL,
       process_name VARCHAR(255) NOT NULL,
       description TEXT,
       version INTEGER DEFAULT 1,
       business_app_name VARCHAR(255) NOT NULL,
       candidate_group_mappings JSONB,
       task_queue_mappings JSONB,
       metadata JSONB,
       active BOOLEAN DEFAULT TRUE,
       deployed BOOLEAN DEFAULT FALSE,
       deployment_id VARCHAR(255),
       created_by VARCHAR(255),
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
   );
   ```

2. **queue_tasks**
   ```sql
   CREATE TABLE queue_tasks (
       task_id VARCHAR(255) PRIMARY KEY,
       process_instance_id VARCHAR(255) NOT NULL,
       process_definition_key VARCHAR(255) NOT NULL,
       task_definition_key VARCHAR(255) NOT NULL,
       task_name VARCHAR(255),
       queue_name VARCHAR(255) NOT NULL,
       assignee VARCHAR(255),
       status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
       priority INTEGER DEFAULT 50,
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       claimed_at TIMESTAMP,
       completed_at TIMESTAMP,
       task_data JSONB,
       INDEX idx_queue_tasks_queue_status (queue_name, status),
       INDEX idx_queue_tasks_assignee (assignee),
       INDEX idx_queue_tasks_process (process_instance_id)
   );
   ```

#### Flowable Standard Tables
- **ACT_RE_*** - Repository tables (process definitions, deployments, models)
- **ACT_RU_*** - Runtime tables (process instances, tasks, variables, jobs)
- **ACT_HI_*** - History tables (completed processes, tasks, variables)
- **ACT_ID_*** - Identity tables (users, groups, memberships)
- **ACT_DMN_*** - DMN decision tables and instances

### REST Endpoints

#### Workflow Metadata Management
- `POST /api/{businessApp}/workflow-metadata/register` - Register workflow with queue mappings
- `POST /api/{businessApp}/workflow-metadata/deploy` - Deploy BPMN process
- `GET /api/{businessApp}/workflow-metadata/{processKey}` - Get workflow metadata
- `PUT /api/{businessApp}/workflow-metadata/{processKey}` - Update metadata
- `DELETE /api/{businessApp}/workflow-metadata/{processKey}` - Delete metadata

#### Process Instance Management
- `POST /api/{businessApp}/process-instances/start` - Start process instance
- `GET /api/{businessApp}/process-instances/{instanceId}` - Get process details
- `GET /api/{businessApp}/process-instances` - List process instances
- `DELETE /api/{businessApp}/process-instances/{instanceId}` - Terminate process

#### Task Management
- `GET /api/{businessApp}/tasks/queue/{queueName}` - Get queue tasks
- `GET /api/{businessApp}/tasks/my-tasks` - Get user's assigned tasks
- `GET /api/{businessApp}/tasks/{taskId}` - Get task details with form data
- `POST /api/{businessApp}/tasks/{taskId}/claim` - Claim task
- `POST /api/{businessApp}/tasks/{taskId}/complete` - Complete task
- `POST /api/{businessApp}/tasks/{taskId}/unclaim` - Release task

#### Queue Management
- `GET /api/{businessApp}/queues` - List available queues
- `GET /api/{businessApp}/queues/{queueName}/stats` - Queue statistics
- `GET /api/{businessApp}/queues/{queueName}/next` - Get next available task

## OneCMS Service

### Overview
The OneCMS Service manages the complete case lifecycle including allegations, entities, and narratives. It integrates with the workflow engine for process orchestration and entitlement service for authorization.

### High-Level Design
- **Technology**: Spring Boot 3.3.4, Spring Data JPA, PostgreSQL
- **Port**: 8083
- **Database**: PostgreSQL (Schema: onecms)
- **Purpose**: Case management domain logic, business operations

### Low-Level Design

#### Key Components
1. **Case Management**: Complete case lifecycle with workflow integration
2. **Allegation Management**: Individual allegation tracking and categorization
3. **Entity Management**: People and organizations involved in cases
4. **Narrative Management**: Case documentation, notes, and reports
5. **Reference Data**: Departments, case types, lookup data management
6. **Workflow Integration**: Seamless process orchestration via workflow service
7. **Authorization Integration**: Permission checks via entitlement service

#### Architecture Layers
```
┌─────────────────────────────────────┐
│           Controllers               │
│ CaseManagementController            │
│ AllegationController, etc.          │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│            Services                 │
│ CaseWorkflowService                │
│ AllegationService, EntityService    │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│        Service Clients              │
│ WorkflowServiceClient              │
│ EntitlementServiceClient           │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│          Repositories               │
│     JPA Repositories                │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│          Database                   │
│      PostgreSQL (onecms)            │
└─────────────────────────────────────┘
```

#### Service Integration Patterns
- **Workflow Service Client**: Reactive WebClient with circuit breaker
- **Entitlement Service Client**: Authorization checks with caching
- **Circuit Breaker**: Resilience4j for fault tolerance
- **Retry Mechanism**: Exponential backoff for transient failures

### Database Schema (OneCMS)

#### ER Diagram
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   departments   │    │   case_types    │    │     cases       │
├─────────────────┤    ├─────────────────┤    ├─────────────────┤
│ id (PK)         │    │ id (PK)         │    │ id (PK)         │
│ department_name │    │ type_name       │    │ case_number     │
│ description     │    │ description     │    │ title           │
│ manager_email   │    │ active          │    │ description     │
│ active          │    │ created_at      │    │ status          │
│ created_at      │    │ updated_at      │    │ priority        │
│ updated_at      │    └─────────────────┘    │ severity        │
└─────────────────┘                           │ case_type_id(FK)│
         │                                    │ department_id(FK)│
         │                                    │ created_by      │
         └────────────────────────────────────│ assigned_to     │
                                              │ process_inst_id │
                                              │ created_at      │
                                              │ updated_at      │
                                              └─────────────────┘
                                                       │
                    ┌──────────────────────────────────┼──────────────────────────────────┐
                    │                                  │                                  │
            ┌───────▼────────┐              ┌─────────▼────────┐              ┌─────────▼────────┐
            │  allegations   │              │  case_entities   │              │ case_narratives  │
            ├────────────────┤              ├──────────────────┤              ├──────────────────┤
            │ id (PK)        │              │ id (PK)          │              │ id (PK)          │
            │ case_id (FK)   │              │ case_id (FK)     │              │ case_id (FK)     │
            │ allegation_type│              │ entity_type      │              │ narrative_type   │
            │ description    │              │ entity_name      │              │ content          │
            │ severity       │              │ entity_id        │              │ author           │
            │ status         │              │ role             │              │ created_at       │
            │ created_at     │              │ details          │              │ updated_at       │
            │ updated_at     │              │ created_at       │              └──────────────────┘
            └────────────────┘              │ updated_at       │
                                            └──────────────────┘
                                                       │
                                            ┌─────────▼────────┐
                                            │ case_comments    │
                                            ├──────────────────┤
                                            │ id (PK)          │
                                            │ case_id (FK)     │
                                            │ comment          │
                                            │ author           │
                                            │ comment_type     │
                                            │ visibility       │
                                            │ created_at       │
                                            │ updated_at       │
                                            └──────────────────┘
```

#### Tables Detail

1. **cases** - Main case entity
   ```sql
   CREATE TABLE cases (
       id BIGSERIAL PRIMARY KEY,
       case_number VARCHAR(50) UNIQUE NOT NULL,
       title VARCHAR(500) NOT NULL,
       description TEXT,
       status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
       priority VARCHAR(20) DEFAULT 'MEDIUM',
       severity VARCHAR(20) DEFAULT 'MEDIUM',
       case_type_id BIGINT REFERENCES case_types(id),
       department_id BIGINT REFERENCES departments(id),
       created_by VARCHAR(255) NOT NULL,
       assigned_to VARCHAR(255),
       process_instance_id VARCHAR(64),
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       INDEX idx_cases_status (status),
       INDEX idx_cases_assigned_to (assigned_to),
       INDEX idx_cases_process_instance (process_instance_id)
   );
   ```

2. **allegations** - Case allegations
   ```sql
   CREATE TABLE allegations (
       id BIGSERIAL PRIMARY KEY,
       case_id BIGINT NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
       allegation_type VARCHAR(100) NOT NULL,
       description TEXT NOT NULL,
       severity VARCHAR(20) DEFAULT 'MEDIUM',
       status VARCHAR(50) DEFAULT 'OPEN',
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       INDEX idx_allegations_case_id (case_id),
       INDEX idx_allegations_type (allegation_type)
   );
   ```

3. **case_entities** - People/Organizations involved
   ```sql
   CREATE TABLE case_entities (
       id BIGSERIAL PRIMARY KEY,
       case_id BIGINT NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
       entity_type VARCHAR(50) NOT NULL, -- PERSON, ORGANIZATION
       entity_name VARCHAR(255) NOT NULL,
       entity_id VARCHAR(255), -- External ID
       role VARCHAR(100), -- COMPLAINANT, RESPONDENT, WITNESS
       details JSONB, -- Additional entity information
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       INDEX idx_case_entities_case_id (case_id),
       INDEX idx_case_entities_type (entity_type)
   );
   ```

### REST Endpoints

#### Case Management
- `POST /api/cms/cases` - Create new case with workflow integration
- `GET /api/cms/cases` - List cases with filtering (status, priority, assignee)
- `GET /api/cms/cases/{caseNumber}` - Get case details with related entities
- `PUT /api/cms/cases/{caseNumber}` - Update case information
- `DELETE /api/cms/cases/{caseNumber}` - Delete case (soft delete)
- `POST /api/cms/cases/{caseNumber}/submit` - Submit case and start workflow
- `GET /api/cms/cases/dashboard-stats` - Dashboard statistics and metrics

#### Allegation Management  
- `POST /api/cms/cases/{caseNumber}/allegations` - Add allegation to case
- `GET /api/cms/cases/{caseNumber}/allegations` - Get case allegations
- `PUT /api/cms/allegations/{id}` - Update allegation
- `DELETE /api/cms/allegations/{id}` - Delete allegation

#### Entity Management
- `POST /api/cms/cases/{caseNumber}/entities` - Add entity to case
- `GET /api/cms/cases/{caseNumber}/entities` - Get case entities
- `PUT /api/cms/entities/{id}` - Update entity information
- `DELETE /api/cms/entities/{id}` - Remove entity from case

#### Narrative Management
- `POST /api/cms/cases/{caseNumber}/narratives` - Add narrative
- `GET /api/cms/cases/{caseNumber}/narratives` - Get case narratives
- `PUT /api/cms/narratives/{id}` - Update narrative
- `DELETE /api/cms/narratives/{id}` - Delete narrative

#### Reference Data
- `GET /api/cms/departments` - List all departments
- `GET /api/cms/case-types` - List all case types
- `GET /api/cms/reference-data/all` - Get all reference data for UI

## React UI Application

### Overview
The React UI Application (CMS-UI-App) provides a modern, responsive web interface for case management, workflow operations, and user administration with role-based UI rendering.

### High-Level Design
- **Technology**: React 18, TypeScript 5, Vite, TailwindCSS
- **Port**: 3000 (Development), 80/443 (Production)
- **Purpose**: User interface for all system functionality
- **Integration**: Consumes APIs via API Gateway with session-based authentication

### Low-Level Design

#### Key Features
1. **Authentication Module**: Session-based login with role-based routing
2. **Case Management Module**: Full case lifecycle with workflow integration
3. **Dashboard Module**: Real-time analytics, task queues, metrics
4. **Task Management Module**: Personal and team task queues
5. **User Administration**: User management with role assignment
6. **Responsive Design**: Mobile-first approach with PWA capabilities

#### Architecture Layers
```
┌─────────────────────────────────────┐
│        React Components             │
│  Pages, Forms, Tables, Modals       │
│  Shadcn/ui + Custom Components      │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│      State Management               │
│  TanStack Query + Zustand           │
│  React Hook Form + Zod Validation   │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│         API Layer                   │
│  Axios + React Query Integration    │
│  Session ID Management              │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│        API Gateway                  │
│       (Port 8080)                   │
└─────────────────────────────────────┘
```

#### Component Architecture
- **Atomic Design**: Atoms, molecules, organisms, templates, pages
- **Shadcn/ui**: Modern component library with Radix UI primitives
- **Form Handling**: React Hook Form with Zod schema validation
- **Data Fetching**: TanStack Query for server state management
- **Client State**: Zustand for client-side state management

### Key UI Modules

#### Dashboard
- **Case Statistics**: Open, in-progress, completed cases by department
- **Task Queue Monitoring**: Real-time task counts by queue and priority
- **Recent Activity**: Activity feed with user actions and system events  
- **Performance Metrics**: SLA compliance, average resolution times

#### Case Management
- **Case List**: Advanced filtering, sorting, and pagination
- **Case Details**: Tabbed interface (Details, Allegations, Entities, Narratives, History)
- **Case Creation**: Multi-step wizard with validation
- **Workflow Integration**: Real-time workflow status and task assignment

#### Task Management
- **My Tasks**: Personal task queue with claim/complete actions
- **Team Queues**: Department task queues with workload balancing
- **Task Details**: Rich task forms with validation and file uploads
- **Task History**: Complete audit trail with timeline view

## Database Design

### Cross-Service Data Relationships

#### Service Data Flow
```
┌─────────────────────────────────────────────────────────────────────┐
│                    Data Flow Architecture                           │
└─────────────────────────────────────────────────────────────────────┘

Entitlements DB                  Flowable DB                  OneCMS DB
┌─────────────────┐             ┌─────────────────┐         ┌─────────────────┐
│     users       │             │workflow_metadata│         │     cases       │
│  ┌──────────────┤             │                 │         │                 │
│  │ id (user123) │             │ process_def_key │         │ case_number     │
│  │ username     │────────────▶│ business_app    │◀────────│ process_inst_id │
│  │ department   │             │ queue_mappings  │         │ created_by      │
│  │ roles[]      │             │                 │         │ assigned_to     │
│  └──────────────│             └─────────────────┘         └─────────────────┘
│                 │                       │                           │
│business_app_    │                       ▼                           │
│roles            │             ┌─────────────────┐                   │
│  ┌──────────────┤             │  queue_tasks    │                   │
│  │ role_name    │             │                 │                   │
│  │ business_app │             │ task_id         │                   │
│  │ permissions  │             │ process_inst_id │◀──────────────────┘
│  └──────────────│             │ queue_name      │
└─────────────────┘             │ assignee        │
                                │ status          │
                                └─────────────────┘
```

## Deployment Architecture

### Container Architecture
```
┌─────────────────────────────────────────────────────────────────────┐
│                        Docker Compose Stack                         │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   React UI      │  │   API Gateway   │  │ Service Registry│
│   (nginx:80)    │  │   (8080)        │  │   (8761)        │
└─────────────────┘  └─────────────────┘  └─────────────────┘

┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ Entitlement     │  │ Workflow Engine │  │ OneCMS Service  │
│ Service (8081)  │  │   (8082)        │  │   (8083)        │
└─────────────────┘  └─────────────────┘  └─────────────────┘

┌─────────────────┐  ┌─────────────────┐  
│   PostgreSQL    │  │     Cerbos      │  
│   (5432)        │  │   (3593)        │  
└─────────────────┘  └─────────────────┘ 
```

## Authentication Architecture

### Session-Based Authentication Flow

The system uses a simplified session-based authentication approach instead of JWT tokens:

```
┌─────────────────────────────────────────────────────────────────┐
│                    Authentication Flow                          │
└─────────────────────────────────────────────────────────────────┘

1. User Login → POST /api/auth/login
   ↓
2. Entitlement Service validates credentials
   ↓
3. Generate UUID session ID and store session info
   ↓
4. Return sessionId to client (stored in localStorage)
   ↓
5. Client includes X-Session-Id header in all requests
   ↓
6. API Gateway validates session with Entitlement Service
   ↓
7. Gateway injects X-User-Id header to downstream services
   ↓
8. Services use X-User-Id for authorization and business logic
```

### Key Benefits of Session-Based Approach

1. **Simplicity**: No JWT token complexity or expiration handling
2. **Security**: Sessions can be immediately invalidated server-side
3. **Performance**: Faster session validation vs JWT verification
4. **Scalability**: Session storage can be distributed (Redis, database)
5. **Debugging**: Clear session tracking and user identification
6. **Integration**: Easy header-based service communication

## Summary

The NextGen Workflow Microservices Architecture provides a comprehensive, scalable solution for case management with the following key benefits:

1. **Scalability**: Independent scaling of services based on demand
2. **Maintainability**: Clear separation of concerns and bounded contexts
3. **Security**: Session-based authentication with fine-grained Cerbos authorization
4. **Flexibility**: Configurable workflows and business rules
5. **Observability**: Comprehensive monitoring and logging
6. **Resilience**: Circuit breakers and fault tolerance patterns
7. **Simplicity**: Streamlined authentication flow with header propagation

The architecture supports complex case management workflows while maintaining clean separation between services and providing a modern, responsive user interface with simplified authentication.