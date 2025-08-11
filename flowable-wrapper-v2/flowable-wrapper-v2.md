# Flowable Wrapper V2 - Comprehensive Documentation

## Table of Contents
1. [Application Overview](#application-overview)
2. [Architecture](#architecture)
3. [Technology Stack](#technology-stack)
4. [Database Structure](#database-structure)
5. [Core Entities](#core-entities)
6. [API Endpoints](#api-endpoints)
7. [Controllers](#controllers)
8. [Services](#services)
9. [DTOs](#dtos)
10. [Security & Authorization](#security--authorization)
11. [Flowable Integration](#flowable-integration)
12. [Cerbos Integration](#cerbos-integration)
13. [Queue System](#queue-system)
14. [Validation Pattern](#validation-pattern)
15. [Configuration](#configuration)
16. [Deployment](#deployment)

## Application Overview

The Flowable Wrapper V2 is a sophisticated queue-centric workflow management platform built on Spring Boot that wraps the Flowable BPMN engine with a custom abstraction layer. It implements a Four-Eyes Principle authorization system using Cerbos for fine-grained access control, maintains event-driven synchronization between Flowable and custom database tables, and provides a multi-tenant business application structure with regional user organization.

### Key Features
- **Queue-Based Task Distribution**: Automatic task routing based on candidate group mappings
- **Multi-Tenant Architecture**: Business application isolation with role-based access
- **Four-Eyes Principle**: Bidirectional enforcement preventing same user from handling maker/checker roles
- **Cerbos Authorization**: Fine-grained access control with policy-based permissions
- **Validation Pattern**: Immediate feedback on task validation failures with retry mechanism
- **Regional Access Control**: Global vs regional user restrictions
- **Event-Driven Synchronization**: Real-time sync between Flowable and custom queue tables

## Architecture

### System Architecture
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Backend       │    │   Database      │
│   (Client)      │◄──►│   (Spring Boot) │◄──►│   (PostgreSQL)  │
│                 │    │   Port: 8090    │    │   Port: 5430    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │   Flowable      │
                       │   (BPMN Engine) │
                       └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │   Cerbos        │
                       │   (AuthZ)       │
                       │   Port: 3592    │
                       └─────────────────┘
```

### Application Layers
1. **Presentation Layer**: REST Controllers with business app path parameters
2. **Business Logic Layer**: Services with queue abstraction
3. **Data Access Layer**: JPA Repositories with JSONB support
4. **Security Layer**: Header-based authentication + Cerbos authorization
5. **Workflow Layer**: Embedded Flowable BPMN Engine
6. **Database Layer**: PostgreSQL with custom and Flowable tables

### Service Layer Architecture
The system follows a clean service-oriented architecture:

1. **TaskService** - Orchestrates task operations and bridges Flowable with queue management
2. **QueueTaskService** - Manages the custom queue abstraction over Flowable tasks
3. **ProcessInstanceService** - Handles workflow instance lifecycle
4. **WorkflowMetadataService** - Manages workflow registration and deployment
5. **UserManagementService** - Handles user and role management
6. **CerbosService** - Provides authorization through Cerbos integration

## Technology Stack

### Core Technologies
- **Java 21**: Programming language
- **Spring Boot 3.3.4**: Application framework
- **Spring Data JPA**: Data persistence
- **PostgreSQL**: Primary database with JSONB support
- **Maven**: Build and dependency management

### Workflow & Authorization
- **Flowable 7.1.0**: Embedded BPMN workflow engine
- **Cerbos SDK 0.16.0**: Fine-grained authorization engine
- **Groovy 3.0.17**: Script task execution

### Additional Libraries
- **Hibernate**: ORM framework with JSONB support
- **Hypersistence Utils 3.9.10**: JSONB type handling
- **SpringDoc OpenAPI 2.3.0**: API documentation
- **Lombok**: Code generation

## Database Structure

### Custom Tables

#### Users Table
```sql
CREATE TABLE users (
    id VARCHAR(50) PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    attributes JSONB
);
```

#### Business Applications Table
```sql
CREATE TABLE business_applications (
    id BIGSERIAL PRIMARY KEY,
    business_app_name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    metadata JSONB,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
```

#### Business App Roles Table
```sql
CREATE TABLE business_app_roles (
    id BIGSERIAL PRIMARY KEY,
    business_app_id BIGINT REFERENCES business_applications(id),
    role_name VARCHAR(100) NOT NULL,
    role_display_name VARCHAR(200),
    description TEXT,
    metadata JSONB,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
```

#### User Business App Roles Table
```sql
CREATE TABLE user_business_app_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(50) REFERENCES users(id),
    business_app_role_id BIGINT REFERENCES business_app_roles(id),
    is_active BOOLEAN DEFAULT TRUE,
    assigned_at TIMESTAMP DEFAULT NOW(),
    assigned_by VARCHAR(50)
);
```

#### Workflow Metadata Table
```sql
CREATE TABLE workflow_metadata (
    id BIGSERIAL PRIMARY KEY,
    process_definition_key VARCHAR(255) UNIQUE NOT NULL,
    process_name VARCHAR(255) NOT NULL,
    description TEXT,
    version INTEGER DEFAULT 1,
    business_app_id BIGINT REFERENCES business_applications(id),
    candidate_group_mappings JSONB NOT NULL,
    task_queue_mappings JSONB,
    metadata JSONB,
    active BOOLEAN DEFAULT TRUE,
    deployed BOOLEAN DEFAULT FALSE,
    deployment_id VARCHAR(255),
    created_by VARCHAR(50) DEFAULT 'system',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

#### Queue Tasks Table
```sql
CREATE TABLE queue_tasks (
    task_id VARCHAR(255) PRIMARY KEY,
    process_instance_id VARCHAR(255) NOT NULL,
    process_definition_key VARCHAR(255) NOT NULL,
    task_definition_key VARCHAR(255) NOT NULL,
    task_name VARCHAR(255) NOT NULL,
    queue_name VARCHAR(255) NOT NULL,
    assignee VARCHAR(50),
    status VARCHAR(50) NOT NULL,
    priority INTEGER DEFAULT 50,
    created_at TIMESTAMP NOT NULL,
    claimed_at TIMESTAMP,
    completed_at TIMESTAMP,
    task_data JSONB
);
```

### Key Design Features
- **JSONB columns** for flexible metadata storage
- **Foreign key relationships** maintaining referential integrity
- **Indexed queries** for performance optimization
- **Support for multi-tenancy** through business applications

## Core Entities

### User Entity
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @Column(name = "id", length = 50)
    private String id;
    
    @Column(name = "username", length = 100, nullable = false, unique = true)
    private String username;
    
    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;
    
    @Column(name = "first_name", length = 100, nullable = false)
    private String firstName;
    
    @Column(name = "last_name", length = 100, nullable = false)
    private String lastName;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Type(io.hypersistence.utils.hibernate.type.json.JsonType.class)
    @Column(name = "attributes", columnDefinition = "jsonb")
    private Map<String, Object> attributes = new HashMap<>();
    
    // Timestamps and other fields...
}
```

### QueueTask Entity
```java
@Entity
@Table(name = "queue_tasks")
public class QueueTask {
    @Id
    @Column(name = "task_id")
    private String taskId;
    
    @Column(name = "process_instance_id", nullable = false)
    private String processInstanceId;
    
    @Column(name = "process_definition_key", nullable = false)
    private String processDefinitionKey;
    
    @Column(name = "task_definition_key", nullable = false)
    private String taskDefinitionKey;
    
    @Column(name = "task_name", nullable = false)
    private String taskName;
    
    @Column(name = "queue_name", nullable = false)
    private String queueName;
    
    @Column(name = "assignee")
    private String assignee;
    
    @Enumerated(EnumType.STRING)
    private TaskStatus status = TaskStatus.OPEN;
    
    @Column(name = "priority")
    private Integer priority = 50;
    
    @Type(JsonBinaryType.class)
    @Column(name = "task_data", columnDefinition = "jsonb")
    private Map<String, Object> taskData;
    
    // Timestamps and other fields...
}
```

### WorkflowMetadata Entity
```java
@Entity
@Table(name = "workflow_metadata")
public class WorkflowMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "process_definition_key", nullable = false, unique = true)
    private String processDefinitionKey;
    
    @Column(name = "process_name", nullable = false)
    private String processName;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_app_id", nullable = false)
    private BusinessApplication businessApplication;
    
    @Type(JsonType.class)
    @Column(name = "candidate_group_mappings", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> candidateGroupMappings;
    
    @Type(JsonType.class)
    @Column(name = "task_queue_mappings", columnDefinition = "jsonb")
    private List<TaskQueueMapping> taskQueueMappings;
    
    // Additional fields...
}
```

### Enums
```java
public enum TaskStatus {
    OPEN("OPEN"),
    CLAIMED("CLAIMED"),
    COMPLETED("COMPLETED");
    
    private final String value;
    
    TaskStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}
```

## API Endpoints

### Task Management Endpoints
- `GET /api/{businessAppName}/tasks/queue/{queueName}` - Get tasks by queue
- `GET /api/{businessAppName}/tasks/my-tasks` - Get user's assigned tasks
- `GET /api/{businessAppName}/tasks/{taskId}` - Get task details
- `POST /api/{businessAppName}/tasks/{taskId}/claim` - Claim a task
- `POST /api/{businessAppName}/tasks/{taskId}/complete` - Complete a task
- `POST /api/{businessAppName}/tasks/{taskId}/unclaim` - Unclaim a task
- `GET /api/{businessAppName}/tasks/queue/{queueName}/next` - Get next available task

### Process Instance Endpoints
- `POST /api/{businessAppName}/process-instances/start` - Start new process instance
- `GET /api/{businessAppName}/process-instances/{processInstanceId}` - Get process instance details

### Workflow Metadata Endpoints
- `POST /api/{businessAppName}/workflow-metadata/register` - Register workflow metadata
- `POST /api/{businessAppName}/workflow-metadata/deploy` - Deploy BPMN workflow
- `GET /api/{businessAppName}/workflow-metadata/{processDefinitionKey}` - Get workflow metadata
- `POST /api/{businessAppName}/workflow-metadata/deploy-from-file` - Deploy from file

### User Management Endpoints
- `GET /api/{businessAppName}/users/{userId}` - Get user details
- `GET /api/{businessAppName}/users/{userId}/roles` - Get user roles
- `POST /api/{businessAppName}/users/{userId}/roles` - Assign user roles

### Health & Monitoring Endpoints
- `GET /actuator/health` - Application health check
- `GET /actuator/info` - Application information
- `GET /actuator/metrics` - Application metrics
- `GET /swagger-ui.html` - API documentation

## Controllers

### TaskController
**Purpose**: Task management and queue operations
**Key Methods**:
- `getTasksByQueue()`: Retrieve tasks from specific queue with authorization
- `getMyTasks()`: Get user's assigned tasks across all queues
- `getTaskDetails()`: Get detailed task information including form data
- `claimTask()`: Claim unassigned task with Four-Eyes validation
- `completeTask()`: Complete task with validation pattern support
- `unclaimTask()`: Release claimed task back to queue
- `getNextTaskFromQueue()`: Get next available task (priority-based)

**Authorization**: All methods use Cerbos for fine-grained access control

### ProcessInstanceController
**Purpose**: Process instance lifecycle management
**Key Methods**:
- `startProcess()`: Start new workflow instance with authorization
- `getProcessInstance()`: Get process instance details with access control

**Authorization**: Cerbos-based authorization for workflow instance operations

### WorkflowMetadataController
**Purpose**: Workflow registration and deployment
**Key Methods**:
- `registerWorkflowMetadata()`: Register workflow with queue mappings
- `deployWorkflow()`: Deploy BPMN to Flowable engine
- `getWorkflowMetadata()`: Retrieve workflow metadata
- `deployWorkflowFromFile()`: Deploy from mounted file system

**Authorization**: Requires deployer or workflow-admin roles

### UserController
**Purpose**: User and role management
**Key Methods**:
- `getUserDetails()`: Get user information with roles
- `getUserRoles()`: Get user's business app roles
- `assignUserRoles()`: Assign roles to users

## Services

### TaskService
**Purpose**: Core task orchestration between Flowable and queue system
**Key Responsibilities**:
- Task lifecycle management (claim, complete, unclaim)
- Integration between Flowable tasks and queue_tasks table
- Validation pattern detection and response
- Task completion with next task population
- Authorization integration with Cerbos

**Key Methods**:
- `claimTask()`: Claim task with authorization and queue update
- `completeTask()`: Complete task with validation failure detection
- `getTaskDetails()`: Get comprehensive task information
- `getTasksByQueue()`: Retrieve tasks by queue with filtering
- `getTasksByAssignee()`: Get user's assigned tasks

### QueueTaskService
**Purpose**: Queue abstraction over Flowable tasks
**Key Responsibilities**:
- Queue task population from Flowable process instances
- Task-to-queue mapping based on candidate groups
- Priority-based task ordering
- Task status management (OPEN, CLAIMED, COMPLETED)
- Event-driven synchronization with Flowable

**Key Methods**:
- `populateQueueTasksForProcessInstance()`: Create queue tasks for new process
- `getTasksByQueue()`: Get tasks by queue with pagination
- `claimTask()`: Update queue task status and assignee
- `completeTask()`: Mark task as completed in queue
- `getNextTaskFromQueue()`: Get highest priority unassigned task

### WorkflowMetadataService
**Purpose**: Workflow registration and deployment management
**Key Responsibilities**:
- Workflow metadata registration with queue mappings
- BPMN deployment to Flowable engine
- Task-to-queue mapping extraction from BPMN
- Deployment lifecycle management
- File-based deployment support

**Key Methods**:
- `registerWorkflowMetadata()`: Register workflow with candidate group mappings
- `deployWorkflow()`: Deploy BPMN and build task mappings
- `buildTaskQueueMappings()`: Extract tasks from BPMN and map to queues
- `deployWorkflowFromFile()`: Deploy from mounted file system

### ProcessInstanceService
**Purpose**: Process instance lifecycle management
**Key Responsibilities**:
- Process instance creation and management
- Integration with queue task population
- Process variable management
- Business key handling

### UserManagementService
**Purpose**: User and role management
**Key Responsibilities**:
- User CRUD operations
- Role assignment and management
- Business application role mapping
- User attribute management

### CerbosService
**Purpose**: Authorization integration
**Key Responsibilities**:
- Principal context building from user data
- Resource context construction
- Policy evaluation for various actions
- Four-Eyes Principle enforcement
- Queue access control
- Regional access restrictions

## DTOs

### Request DTOs
- `StartProcessRequest`: Process instance creation
- `CompleteTaskRequest`: Task completion with variables
- `RegisterWorkflowMetadataRequest`: Workflow registration
- `DeployWorkflowRequest`: BPMN deployment
- `AssignRoleRequest`: User role assignment

### Response DTOs
- `ProcessInstanceResponse`: Process instance information
- `QueueTaskResponse`: Queue task details
- `TaskDetailResponse`: Comprehensive task information
- `TaskCompletionResponse`: Task completion result with validation status
- `WorkflowMetadataResponse`: Workflow metadata with mappings
- `UserResponse`: User information with roles

### User DTOs
- `UserDetailsResponse`: User with business app roles
- `UserRoleResponse`: User role assignment information
- `BusinessAppRoleResponse`: Business application role details

## Security & Authorization

### Authentication
- **Header-based authentication**: User ID passed via `X-User-Id` header
- **No password validation**: Assumes external authentication system
- **User context extraction**: Automatic user ID validation from headers

### Authorization Architecture
The system implements a sophisticated authorization model using Cerbos:

**Principal Context**:
- User ID, roles, and attributes (region, department, business apps)
- Dynamic role extraction from database
- Regional access control support

**Resource Context**:
- Business application and process definition key
- Process instance context with variables
- Task-specific context including queue information
- Historical task states for Four-Eyes Principle enforcement

### Four-Eyes Principle Implementation
```yaml
# Four-Eyes Principle: Bidirectional check
- expr: >
    !(
      (request.resource.attr.currentTask.taskDefinitionKey == "l1_checker_review_task" && 
       has(request.resource.attr.taskStates.l1_maker_review_task) &&
       request.resource.attr.taskStates.l1_maker_review_task.assignee == request.principal.id) ||
      (request.resource.attr.currentTask.taskDefinitionKey == "l1_maker_review_task" && 
       has(request.resource.attr.taskStates.l1_checker_review_task) &&
       request.resource.attr.taskStates.l1_checker_review_task.assignee == request.principal.id)
    )
```

### Authorization Enforcement Points
- **Controller Level**: All endpoints validate authorization before processing
- **Service Level**: CerbosService provides centralized authorization logic
- **Fine-grained Actions**: `claim_task`, `complete_task`, `view_task`, `view_queue`, `start_workflow_instance`

### Multi-Tenant Security
- **Business Application Isolation**: All APIs require businessAppName path parameter
- **Role-Based Access**: Users have different roles in different business applications
- **Regional Restrictions**: Global vs regional user access control

## Flowable Integration

### Embedded Flowable Engine Configuration
```yaml
flowable:
  database-schema-update: true
  async-executor-activate: false
  history-level: full
```

### Key Integration Points
- **Process Deployment**: BPMN deployment to embedded Flowable engine
- **Task Management**: Integration between Flowable tasks and custom queue system
- **Process Variables**: Seamless variable passing between systems
- **Event Listeners**: Custom listeners for task lifecycle events

### Workflow Services Integration
- `RepositoryService`: Process definition management and BPMN parsing
- `RuntimeService`: Process instance management and variable handling
- `TaskService`: Native Flowable task operations
- `FormService`: Form data extraction and management

### BPMN Task Extraction
The system automatically extracts user tasks from deployed BPMN and maps them to queues:

```java
private List<TaskQueueMapping> buildTaskQueueMappings(String processDefinitionId, 
                                                      Map<String, String> candidateGroupMappings) {
    BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
    Collection<FlowElement> flowElements = bpmnModel.getMainProcess().getFlowElements();
    
    for (FlowElement element : flowElements) {
        if (element instanceof UserTask) {
            UserTask userTask = (UserTask) element;
            List<String> candidateGroups = userTask.getCandidateGroups();
            String assignedQueue = determineQueue(candidateGroups, candidateGroupMappings);
            // Create TaskQueueMapping...
        }
    }
}
```

## Cerbos Integration

### Authorization Architecture
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Application   │    │   Cerbos        │    │   Policies      │
│   (Spring Boot) │◄──►│   (AuthZ)       │◄──►│   (YAML)        │
│                 │    │   Port: 3592    │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Cerbos Configuration
```java
@Configuration
public class CerbosConfig {
    @Bean
    public CerbosBlockingClient cerbosClient() {
        return CerbosClientBuilder.forTarget(cerbosEndpoint)
                .withPlaintext()
                .buildBlockingClient();
    }
}
```

### Policy Structure Example
```yaml
apiVersion: api.cerbos.dev/v1
resourcePolicy:
  version: "default"
  resource: "Sanctions-Management::sanctionsCaseManagement"
  rules:
    - actions: ["claim_task", "complete_task"]
      effect: EFFECT_ALLOW
      roles: ["level1-operator"]
      condition:
        match:
          all:
            of:
              - expr: request.resource.attr.queue in request.principal.attr.queues
              - expr: >
                  !(
                    (request.resource.attr.currentTask.taskDefinitionKey == "l1_checker_review_task" && 
                     has(request.resource.attr.taskStates.l1_maker_review_task) &&
                     request.resource.attr.taskStates.l1_maker_review_task.assignee == request.principal.id)
                  )
```

### Authorization Methods
- **Task Authorization**: `isAuthorizedForTask(userId, action, businessApp, taskId)`
- **Queue Access**: `canAccessQueue(userId, businessApp, queueName)`
- **Workflow Management**: `isAuthorizedForWorkflowManagement(userId, action, processKey, businessApp)`
- **Creation Authorization**: `isAuthorizedForCreation(userId, action, processKey, businessApp, request)`

## Queue System

### Queue Abstraction Architecture
The system creates a queue-centric view over Flowable's task management:

**Core Components**:
- **QueueTask Entity**: Custom queue representation of Flowable tasks
- **QueueTaskService**: Manages queue operations and task lifecycle
- **TaskService**: Orchestrates between Flowable and queue abstraction

### Task Lifecycle Management
```
OPEN -> CLAIMED -> COMPLETED
```

**Key Features**:
- Priority-based task ordering (DESC priority, ASC creation time)
- Assignee tracking with claim/unclaim operations
- Task completion with next task population
- Validation failure detection and retry mechanism

### Candidate Group to Queue Mapping
**Workflow Metadata Structure**:
```json
{
  "candidateGroupMappings": {
    "managers": "manager-queue",
    "finance": "finance-queue",
    "level1-maker": "level1-queue",
    "level1-checker": "level1-queue"
  },
  "taskQueueMappings": [
    {
      "taskId": "approvalTask",
      "taskName": "Approval Task", 
      "candidateGroups": ["managers"],
      "queue": "manager-queue"
    }
  ]
}
```

### Event-Driven Synchronization
- **Process Start**: `populateQueueTasksForProcessInstance()` creates queue tasks
- **Task Completion**: Updates existing task and creates new tasks for next steps
- **Validation Failures**: Detects loopback scenarios and provides retry mechanisms

### Queue Operations
- **Priority Ordering**: Tasks ordered by priority (DESC) then creation time (ASC)
- **Unassigned Task Retrieval**: Filter for tasks without assignee
- **Next Task Selection**: Get highest priority, oldest unassigned task
- **Bulk Operations**: Support for pagination and filtering

## Validation Pattern

### Task Validation Flow
The system implements a standard validation pattern for immediate feedback:

```
┌─────────────┐     ┌────────────────┐     ┌─────────────┐
│  User Task  │────>│  Script Task   │────>│  Gateway    │
│ (Data Entry)│     │ (Validation)   │     │  (Router)   │
└─────────────┘     └────────────────┘     └──────┬──────┘
       ↑                                         │ │
       │ Rejected                                │ │
       └────────────────────┬────────────────────┘ │
                            │                      │ Approved
┌───────────────────┐       │                      ↓
│ Increment Tries   │<──────┘                Next Stage
│ (Script Task)     │
└───────────────────┘
```

### Validation Detection Logic
When `TaskService.completeTask()` is called:

```java
public TaskCompletionResponse completeTask(String taskId, String userId, CompleteTaskRequest request) {
    // 1. Complete the task in Flowable
    flowableTaskService.complete(taskId, variables);
    
    // 2. Check what happened - look for same task reappearing
    List<QueueTaskResponse> nextTasks = queueTaskService.getTasksByProcessInstance(processInstanceId);
    
    // 3. Detect if same task reappeared (validation failed)
    for (QueueTaskResponse nextTask : nextTasks) {
        if (nextTask.getTaskDefinitionKey().equals(taskDefinitionKey)) {
            // Validation failed - task looped back
            return buildValidationFailedResponse(nextTask);
        }
    }
    
    // Normal flow continues...
}
```

### API Response Patterns

**Success Response**:
```json
{
    "status": "COMPLETED",
    "message": "Task completed successfully",
    "processActive": true,
    "nextTaskId": "task-456",
    "nextTaskName": "Manager Approval",
    "nextTaskQueue": "manager-queue"
}
```

**Validation Failed Response**:
```json
{
    "status": "VALIDATION_FAILED",
    "message": "Please correct the errors and resubmit",
    "validationErrors": [
        "Receipt required for expenses over $100",
        "Description must be at least 10 characters"
    ],
    "attemptNumber": 2,
    "retryTaskId": "task-789",
    "processActive": true
}
```

### BPMN Validation Pattern
Standard pattern using script tasks for validation:

```xml
<userTask id="submitData" name="Submit Data" flowable:candidateGroups="level1-makers"/>

<scriptTask id="validateData" name="Validate Submission" scriptFormat="groovy">
  <script>
    <![CDATA[
      def errors = new java.util.ArrayList();
      
      // Validation logic
      if (!execution.hasVariable("amount") || execution.getVariable("amount") == null) {
        errors.add("Amount is required");
      }
      
      // Set results
      if (errors.isEmpty()) {
        execution.setVariable("submitDataValid", true);
      } else {
        execution.setVariable("submitDataValid", false);
        execution.setVariable("submitDataValidationError", errors);
      }
    ]]>
  </script>
</scriptTask>

<exclusiveGateway id="validationGateway" name="Is Valid?" />

<sequenceFlow id="successPath" sourceRef="validationGateway" targetRef="nextStage">
  <conditionExpression>${execution.getVariable('submitDataValid') == true}</conditionExpression>
</sequenceFlow>

<sequenceFlow id="failurePath" sourceRef="validationGateway" targetRef="incrementTries">
  <conditionExpression>${execution.getVariable('submitDataValid') == false}</conditionExpression>
</sequenceFlow>
```

## Configuration

### Application Configuration (application.yml)
```yaml
spring:
  application:
    name: flowable-wrapper-v2
  
  datasource:
    url: jdbc:postgresql://localhost:5430/flowable_wrapper
    username: flowable
    password: flowable
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
  
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    show-sql: false
    defer-datasource-initialization: true
  
  sql:
    init:
      mode: always
      data-locations: classpath:db/data-new.sql
      schema-locations: classpath:db/schema.sql

server:
  port: 8090

flowable:
  database-schema-update: true
  async-executor-activate: false
  history-level: full

cerbos:
  endpoint: ${CERBOS_ENDPOINT:localhost:3592}
  tls:
    enabled: ${CERBOS_TLS_ENABLED:false}

workflow:
  definitions:
    path: ${WORKFLOW_DEFINITIONS_PATH:/app/definitions}
```

### Environment Variables
- `CERBOS_ENDPOINT`: Cerbos service endpoint (default: localhost:3592)
- `CERBOS_TLS_ENABLED`: Enable TLS for Cerbos connection (default: false)
- `WORKFLOW_DEFINITIONS_PATH`: Path to BPMN definition files
- Database connection variables for PostgreSQL

### Docker Compose Configuration
```yaml
version: '3.8'
services:
  flowable-wrapper-v2:
    build: .
    ports:
      - "8090:8090"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/flowable_wrapper
      - CERBOS_ENDPOINT=cerbos:3592
    depends_on:
      - postgres
      - cerbos
  
  postgres:
    image: postgres:15
    ports:
      - "5430:5432"
    environment:
      - POSTGRES_DB=flowable_wrapper
      - POSTGRES_USER=flowable
      - POSTGRES_PASSWORD=flowable
  
  cerbos:
    image: ghcr.io/cerbos/cerbos:latest
    ports:
      - "3592:3592"
    volumes:
      - ./cerbos/policies:/policies
    command: ["server", "--config=/policies/.cerbos.yaml"]
```

## Deployment

### Prerequisites
- Java 21+
- PostgreSQL 15+
- Maven 3.9+
- Docker and Docker Compose
- Cerbos (for authorization)

### Build & Run

#### Local Development
```bash
# Build application
cd flowable-wrapper-v2
mvn clean install

# Run locally
mvn spring-boot:run
```

#### Docker Deployment
```bash
# Navigate to docker directory
cd docker

# Start all services
docker-compose up -d

# Verify services
curl http://localhost:8090/actuator/health
```

### Database Initialization
The application automatically initializes the database with:
- **Schema Creation**: `classpath:db/schema.sql`
- **Data Seeding**: `classpath:db/data-new.sql`
- **Flowable Tables**: Automatically created by Flowable engine

### API Testing Guide

#### Step 1: Register Workflow Metadata
```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/workflow-metadata/register" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: automation-user-2" \
    -d '{
      "processDefinitionKey": "sanctionsCaseManagement",
      "processName": "Sanctions L1-L2 Flow",
      "businessAppName": "Sanctions-Management",
      "candidateGroupMappings": {
        "level1-maker": "level1-queue",
        "level1-checker": "level1-queue",
        "level2-maker": "level2-queue",
        "level2-checker": "level2-queue"
      }
    }'
```

#### Step 2: Deploy BPMN Workflow
```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/workflow-metadata/deploy-from-file?processDefinitionKey=sanctionsCaseManagement&filename=SanctionsL1L2Flow.bpmn20.xml" \
    -H "X-User-Id: automation-user-2"
```

#### Step 3: Start Process Instance
```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/process-instances/start" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: automation-user-2" \
    -d '{
      "processDefinitionKey": "sanctionsCaseManagement",
      "variables": {
        "caseId": "CASE-001",
        "region": "US",
        "matches": [{"matchId": "MATCH-001", "entityName": "John Doe", "score": 0.95}]
      }
    }'
```

#### Step 4: Queue Operations
```bash
# Get tasks from queue
curl -X GET "http://localhost:8090/api/Sanctions-Management/tasks/queue/level1-queue" \
    -H "X-User-Id: us-l1-operator-1"

# Claim task
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/{taskId}/claim" \
    -H "X-User-Id: us-l1-operator-1"

# Complete task
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/{taskId}/complete" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: us-l1-operator-1" \
    -d '{
      "variables": {
        "decision": "approve",
        "comments": "Looks good"
      }
    }'
```

## API Documentation

The application provides comprehensive API documentation:
- **Swagger UI**: `http://localhost:8090/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8090/api-docs`
- **Actuator Health**: `http://localhost:8090/actuator/health`

## Monitoring & Logging

### Actuator Endpoints
- `/actuator/health` - Application health with database status
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics

### Logging Configuration
- **Debug Level**: Flowable components for detailed workflow logging
- **Info Level**: Application and Spring components
- **Console Pattern**: Simplified timestamp and message format
- **SQL Logging**: Hibernate SQL queries (configurable)

### Key Metrics to Monitor
1. **Queue Task Metrics**: Tasks by status and queue
2. **Process Instance Metrics**: Active vs completed processes
3. **Authorization Metrics**: Cerbos policy evaluation times
4. **Database Metrics**: Connection pool usage and query performance

---

**Version**: 2.0.0  
**Last Updated**: January 2025  
**Maintainer**: Flowable Wrapper Development Team

## Summary

The Flowable Wrapper V2 represents a sophisticated enterprise workflow platform that successfully bridges the gap between Flowable's powerful BPMN engine and business-specific requirements. The queue-centric abstraction, combined with Cerbos-based authorization and Four-Eyes Principle implementation, creates a robust, secure, and compliant workflow management system suitable for complex business processes in regulated industries.

Key architectural strengths include:
- **Queue-based task distribution** with automatic routing
- **Multi-tenant business application** structure
- **Fine-grained authorization** with Cerbos integration
- **Validation pattern** for immediate user feedback
- **Event-driven synchronization** between systems
- **Regional access control** for global deployments

The system demonstrates excellent separation of concerns, event-driven design patterns, and comprehensive security measures, making it a solid foundation for enterprise workflow management with advanced authorization requirements.