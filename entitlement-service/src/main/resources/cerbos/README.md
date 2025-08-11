# Cerbos Policy Configuration for NextGen Workflow Application

This directory contains the Cerbos policy definitions for the NextGen Workflow microservices ecosystem.

## Policy Structure

```
cerbos/
├── policies/
│   ├── derived_roles/
│   │   └── one-cms.yaml          # Reusable derived roles for OneCMS application
│   ├── resources/
│   │   ├── case.yaml             # Case resource policy (OneCMS domain)
│   │   └── one-cms-workflow.yaml # Workflow resource policy (Flowable platform)
│   └── principal/                # Future principal policies (if needed)
└── README.md                     # This file
```

## Policy Files

### Derived Roles (`derived_roles/one-cms.yaml`)
Defines reusable derived roles that can be referenced by resource policies:
- `case_assignee`: Users assigned to specific cases
- `case_department_member`: Users belonging to case's department
- `queue_member`: Users with access to specific workflow queues
- `task_assignee`: Users assigned to specific workflow tasks
- `same_department_member`: Users in same department as case

### Case Resource Policy (`resources/case.yaml`)
Governs access to case management operations:
- **Version**: 1.0
- **Resource**: `case`
- **Enforced by**: OneCMS Service
- **Key Actions**: create, read, update, add_allegation, add_narrative, assign, close_case, delete, audit

### Workflow Resource Policy (`resources/one-cms-workflow.yaml`)
Governs access to workflow operations:
- **Version**: 1.0
- **Resource**: `OneCMS::Process_CMS_Workflow_Updated`
- **Enforced by**: Flowable Core Workflow Service
- **Key Actions**: start_workflow_instance, view_queue, claim_task, complete_task, delegate_task

## Principal Attributes

The Entitlement Service automatically populates the following principal attributes when building authorization requests:

### Core Attributes
- `id`: User ID (e.g., "alice.intake")
- `roles`: List of business application roles (e.g., ["INTAKE_ANALYST", "INVESTIGATOR"])

### Department Attributes
- `departments`: List of department codes user belongs to (e.g., ["HR", "LEGAL"])
  - Source: `user_departments` table joined with `departments` table
  - Used by: `case_department_member` derived role

### Queue Attributes  
- `queues`: List of workflow queues user can access (e.g., ["onecms-intake-queue", "onecms-hr-review-queue"])
  - Source: `business_app_roles.metadata.queues` JSONB field
  - Used by: `queue_member` derived role and workflow policies

### Business Application Attributes
- `businessApps`: List of business applications user has access to (e.g., ["OneCMS"])

## Policy Versioning

Policies use **Option A: Resource Policy Versioning**:
- Each resource policy has a `version` field (e.g., "1.0")
- Allows multiple policy versions to coexist
- Enables gradual rollouts and easy rollbacks
- Native Cerbos support for policy versioning

## Environment Configuration

### Development (Docker)
```yaml
cerbos:
  host: localhost
  port: 3593
  tls:
    enabled: false
  policies:
    auto-load: true
    validate-on-startup: true
```

### Production
```yaml
cerbos:
  host: ${CERBOS_PROD_HOST}
  port: 443
  tls:
    enabled: true
  policies:
    auto-load: false  # Policies deployed via CI/CD
    validate-on-startup: true
```

## Policy Loading

### Development
- Policies are automatically validated on service startup
- Policy files are read from classpath resources
- `CerbosPolicyService` validates file existence and readability

### Production
- Policies are deployed to Cerbos PDP via CI/CD pipeline
- Service validates connection to Cerbos but doesn't auto-load policies
- External policy store management

## Usage Examples

### Case Access Check
```java
AuthorizationCheckRequest request = AuthorizationCheckRequest.builder()
    .principal(Principal.builder()
        .id("alice.intake")
        .attributes(Map.of(
            "departments", List.of("INTAKE"),
            "queues", List.of("onecms-intake-queue")
        ))
        .build())
    .resource(Resource.builder()
        .kind("case")
        .id("CASE-2024-001")
        .attributes(Map.of(
            "department_code", "HR",
            "assigneeId", "bob.hr"
        ))
        .build())
    .action("read")
    .build();
```

### Workflow Queue Access Check
```java
AuthorizationCheckRequest request = AuthorizationCheckRequest.builder()
    .principal(Principal.builder()
        .id("charlie.legal")
        .attributes(Map.of(
            "queues", List.of("onecms-legal-review-queue")
        ))
        .build())
    .resource(Resource.builder()
        .kind("OneCMS::Process_CMS_Workflow_Updated")
        .id("workflow-instance-123")
        .attributes(Map.of(
            "currentQueue", "onecms-legal-review-queue"
        ))
        .build())
    .action("view_queue")
    .build();
```

## API Endpoints

The entitlement service provides policy management endpoints:

- `GET /api/policies/status` - Get policy service status and connection info
- `POST /api/policies/validate` - Validate all policy files
- `GET /api/policies/content/{policyType}/{fileName}` - Get policy file content for debugging

## Troubleshooting

### Common Issues

1. **Policy File Not Found**
   - Check file paths in `src/main/resources/cerbos/policies/`
   - Ensure proper directory structure (derived_roles/, resources/)

2. **Cerbos Connection Failed**
   - Verify Cerbos PDP is running (Docker: `docker run -p 3593:3593 ghcr.io/cerbos/cerbos`)
   - Check network connectivity and firewall settings

3. **Policy Validation Failed**
   - Check YAML syntax in policy files
   - Verify required fields (apiVersion, derivedRoles/resourcePolicy, etc.)

4. **Principal Attributes Missing**
   - Check database data (user_departments, business_app_roles.metadata)
   - Verify JSON structure in metadata.queues field

### Debug Steps

1. Check service logs during startup for policy loading messages
2. Use `/api/policies/status` endpoint to verify connectivity
3. Use `/api/policies/validate` endpoint to test policy syntax
4. Check Cerbos PDP logs for policy evaluation details

## Security Notes

- Policy files contain authorization rules - treat as sensitive configuration
- In production, policies should be deployed via secure CI/CD pipeline
- Avoid hardcoding sensitive data in policy files
- Regularly audit and review policy changes