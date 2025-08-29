1. entitlement_application_domains
• Purpose: This table defines logical application or business domains within the entitlement system. It acts as a  op-level organizational unit for grouping related roles and permissions, allowing for clear separation of concerns across different applications or business lines.
• Key Fields:
domain_id (PK, UUID): Unique identifier for the application domain.
domain_name (VARCHAR): A human-readable name for the domain (e.g., "OneCMS", "CSAW").
description (TEXT): A detailed explanation of the domain's purpose.
is_tiered (BOOLEAN): Indicates if the domain has a tiered structure (e.g., for different levels of access within the domain).
domain_metadata (JSONB): Flexible JSON field for storing additional, unstructured metadata related to the domain.
is_active (BOOLEAN): Status of the domain.
created_at, updated_at (TIMESTAMPTZ): Timestamps for record creation and last update.

2. entitlement_domain_roles
• Purpose: This table defines specific roles that exist within a particular application domain. These roles encapsulate a set of permissions that can be assigned to users.
Key Fields:
• role_id (PK, UUID): Unique identifier for the role.
• domain_id (FK, UUID): Foreign key linking the role to its parent application domain.
• role_name (VARCHAR): Unique internal name for the role (e.g., "EO_OFFCIER", "Sanctions_Analyst").
• display_name (VARCHAR): A user-friendly name for the role.
• description (TEXT): A description of the role's responsibilities.
• role_level (ENUM): Defines a hierarchy or level for the role (e.g., "Tier1","Tier2").
• maker_checker_type (ENUM): Specifies if the role is involved in a maker-checker process (e.g., requiring a second person to approve actions).
• role_metadata (JSONB): Flexible JSON field for storing additional role-specific metadata.
• is_active (BOOLEAN): Status of the role.
• created_at, updated _at (TIMESTAMPTZ): Timestamps for record creation and last update.

3. entitlement_permissions
• Purpose: This table defines the individual, granular permissions that can be granted. Permissions are typically defined by a resource type and an action.
• Key Fields:
• permission _id (PK, UUID): Unique identifier for the permission.
• resource_type (VARCHAR): The type of resource to which the permission applies (e.g., "employee_record", "sanction_case", "bank_account").
action (VARCHAR): The specific action that can be performed on the resource (e.g., "read", "write", "delete", "approve", "initiate").
• description (TEXT): A description of what the permission allows.
• created _at, updated _at (TIMESTAMPTZ): Timestamps for record creation and last update

4. entitlement_role_permissions
• Purpose: This is a mapping table that associates specific permissions with specific roles. It forms the core of the Role-Based Access Control (RBAC) model.
• Key Fields:
• role_permission_id (PK, UUID): Unique identifier for this mapping entry.
• role_id (FK, UUID): Foreign key linking to the role.
• permission_id (FK, UUID): Foreign key linking to the permission.
• is_active (BOOLEAN): Status of this role-permission assignment.
• created_at, updated_at (TIMESTAMPTZ): Timestamps for record creation and last update.

5. entitlement_core_users
• Purpose: This table stores fundamental information about the users who will be interacting with the system and whose entitlements are managed.
• Key Fields:
• user_id (PK, UUID): Unique identifier for the user.
• username (VARCHAR): User's unique login username.
• email (VARCHAR): User's email address.
• first_name (VARCHAR): User's first name.
last_name (VARCHAR): User's last name.
• is_active (BOOLEAN): Indicates if the user account is active. global_attributes (JSONB): Flexible JSON field for storing user-specific attributes that might be used in policy evaluation (e.g., department, location, clearance level).
• created _at, updated _at (TIMESTAMPTZ): Timestamps for record creation and last update.

6. entitlement_user_domain_roles
• Purpose: This table assigns specific roles from a domain to individual users.
• Key Fields:
• assignment_id (PK, UUID): Unique identifier for this user-role assignment.
• user_id (FK, UUID): Foreign key linking to the user.
• role_id (FK, UUID): Foreign key linking to the role.
• is_active (BOOLEAN): Status of this assignment.
• assigned_at (TIMESTAMPTZ): Timestamp when the role was assigned.
• assigned_by_user_id (UUID): Identifier of the user who performed the assignment (for auditing).

7. resource_permissions
• Purpose: This table allows for the direct assignment of permissions to a user on a specific resource instance. This is useful for exceptions to role-based access or for highly specific, direct grants.
• Key Fields:
• permission_id (PK, UUID): Unique identifier for this specific resource permission grant.
• resource_type (VARCHAR): The type of resource (e.g., "document","specific_sanction_case_id").
• resource_id (UUID): The unique identifier of the specific resource instance.
• user_id (FK, UUID): Foreign key linking to the user.
• allowed_actions (JSONB): A JSON array or object specifying the actions allowed for this user on this specific resource (e.g., ["read", "edit"]).

8. entitlement_audit_logs
• Purpose: This critical table logs every authorization decision or significant entitlement-related event, providing an immutable record for auditing, compliance, and security analysis.
• Key Fields:
• log id (PK, UUID): Unique identifier for the audit log entry.
• user_id (FK, UUID): The user who attempted or performed the action.
• resource_type (VARCHAR): The type of resource involved. resource_id (VARCHAR): The identifier of the specific resource instance.
• action (VARCHAR): The action attempted (e.g., "access_document" , "approve_transaction").
• timestamp (TIMESTAMPTZ): When the event occurred.
• cerbos_decision (VARCHAR): The decision returned by an external authorization decision engine (e.g., "ALLOW", "DENY"), indicating the outcome of the entitlement check.

9. policies
• Purpose: This table stores the definitions of authorization policies, which are rules that govern access based on various attributes. This supports Policy-Based Access Control (PBAC).
• Key Fields:
• policy_id (PK, UUID): Unique identifier for the policy.
• policy_definition (JSONB): The actual definition of the policy, likely in a structured format (e.g., OPA Rego, Cerbos • policy language snippet, or a custom JSON schema).
created_at, updated_at (TIMESTAMPTZ): Timestamps for record creation and last update.

10. match_conditions
• Purpose: This table stores specific conditions that need to be met for a policy to apply or for an access decision to be made. These conditions leverage attributes of the principal (user), resource, and context.
• Key Fields:
• condition_id (PK, UUID): Unique identifier for the condition.
• principal_attributes (JSONB): JSON object containing attributes related to the
• user (e.g., "department": "HR", "clearance _level": "top_secret").
• resource_attributes (JSONB): JSON object containing attributes related to the resource (e.g., "document_sensitivity": "confidential", "project_status": "active").
• context (JSONB): JSON object containing environmental or contextual attributes (e.g., "time_of_day": "business_hours", "network_location": "internal").
created_at (TIMESTAMPTZ): Timestamp for record creation.

11. entitlement_hierarchical_structures(Optional)
• Purpose: This table models any hierarchical structures relevant to entitlements, such as organizational charts (reporting lines), departmental structures, or logical groups.
• Key Fields:
• node_id (PK, UUID): Unique identifier for a node in the hierarchy. node_name (VARCHAR): Name of the hierarchical node (eg., "CEO Office", "North America Sales", "HR Payroll Dept").
• node_type (ENUM): The type of the node (e.g., "Department", "Team", "ReportingUnit"").
• parent_node_id (FK, UUID): Foreign key linking to the parent node, establishing the hierarchy.
• domain_id (FK, UUID): Foreign key linking the node to a specific application domain.
• is_active (BOOLEAN): Status of the node.
• hierarchy_metadata (JSONB): Flexible JSON field for storing additional metadata about the hierarchical node.
• created_at, updated _at (TIMESTAMPTZ): Timestamps for record creation and last updated time stamp

12. entitlement_user_hierarchy_assignments (Optional)
• Purpose: This table assigns users to specific nodes within the defined hierarchical structures.
• Key Fields:
• assignment_id (PK, UUID): Unique identifier for this assignment.
• user_id (FK, UUID): Foreign key linking to the user.
• node_id (FK, UUID): Foreign key linking to the hierarchical node.
• assignment_type (ENUM): Describes the nature of the assignment (e.g.,"member", "manager", "admin").
• is_active (BOOLEAN): Status of the assignment.
• assigned_at (TIMESTAMPTZ): Timestamp when the user was assigned to thehierarchy node.
• assigned_by_user_id (UUID): Identifier of the user who performed the assignment.


Endpoint Analysis Based on Hybrid_Approach.md Schema

  1. Domain Management Endpoints

  (Multi-tenant application support)

  # Application Domain Management (entitlement_application_domains)
  GET    /api/entitlements/domains                           # List all domains
  POST   /api/entitlements/domains                           # Create new domain
  GET    /api/entitlements/domains/{domainId}                # Get domain details
  PUT    /api/entitlements/domains/{domainId}                # Update domain
  DELETE /api/entitlements/domains/{domainId}                # Delete domain
  PATCH  /api/entitlements/domains/{domainId}/activate       # Activate domain
  PATCH  /api/entitlements/domains/{domainId}/deactivate     # Deactivate domain

  Usage:
  - OneCMS multi-tenant deployments can isolate policies by domain
  - Each client/organization gets their own domain
  - Domain metadata stores client-specific configurations

  2. Role Management Endpoints

  (entitlement_domain_roles)

  # Domain Role Management
  GET    /api/entitlements/domains/{domainId}/roles          # List roles in domain
  POST   /api/entitlements/domains/{domainId}/roles          # Create role in domain
  GET    /api/entitlements/domains/{domainId}/roles/{roleId} # Get role details
  PUT    /api/entitlements/domains/{domainId}/roles/{roleId} # Update role
  DELETE /api/entitlements/domains/{domainId}/roles/{roleId} # Delete role
  PATCH  /api/entitlements/domains/{domainId}/roles/{roleId}/activate   # Activate role
  PATCH  /api/entitlements/domains/{domainId}/roles/{roleId}/deactivate # Deactivate role

  # Role search and filtering
  GET    /api/entitlements/domains/{domainId}/roles?level={tier}        # Filter by tier level
  GET    /api/entitlements/domains/{domainId}/roles?checker_type={type} # Filter by maker/checker

  Usage:
  - Create OneCMS-specific roles like "CASE_INVESTIGATOR", "EO_OFFICER"
  - Support role hierarchies (Tier1, Tier2) for escalation workflows
  - Maker/checker patterns for sensitive operations

  3. Permission Management Endpoints

  (entitlement_permissions, entitlement_role_permissions)

  # System-wide Permissions
  GET    /api/entitlements/permissions                       # List all permissions
  POST   /api/entitlements/permissions                       # Create new permission
  GET    /api/entitlements/permissions/{permissionId}        # Get permission details
  PUT    /api/entitlements/permissions/{permissionId}        # Update permission
  DELETE /api/entitlements/permissions/{permissionId}        # Delete permission

  # Filter permissions by resource type
  GET    /api/entitlements/permissions?resource_type=case    # Case-specific permissions
  GET    /api/entitlements/permissions?resource_type=workflow # Workflow permissions

  # Role-Permission Mapping
  GET    /api/entitlements/roles/{roleId}/permissions        # Get role's permissions
  POST   /api/entitlements/roles/{roleId}/permissions        # Grant permission to role
  DELETE /api/entitlements/roles/{roleId}/permissions/{permissionId} # Revoke permission

  # Bulk operations
  POST   /api/entitlements/roles/{roleId}/permissions/bulk   # Bulk grant permissions
  DELETE /api/entitlements/roles/{roleId}/permissions/bulk   # Bulk revoke permissions

  Usage:
  - Define granular permissions like "case:create", "case:assign", "workflow:start"
  - Map permissions to roles for RBAC implementation
  - Support bulk operations for role templates

  4. User Management Endpoints

  (entitlement_core_users, entitlement_user_domain_roles)

  # User Management
  GET    /api/entitlements/users                             # List all users
  POST   /api/entitlements/users                             # Create new user
  GET    /api/entitlements/users/{userId}                    # Get user details
  PUT    /api/entitlements/users/{userId}                    # Update user
  DELETE /api/entitlements/users/{userId}                    # Delete user
  PATCH  /api/entitlements/users/{userId}/activate           # Activate user
  PATCH  /api/entitlements/users/{userId}/deactivate         # Deactivate user

  # User search and filtering
  GET    /api/entitlements/users?search={query}              # Search users
  GET    /api/entitlements/users?department={dept}           # Filter by department
  GET    /api/entitlements/users?active=true                 # Filter by status

  # User Role Assignments
  GET    /api/entitlements/users/{userId}/roles              # Get user's roles
  POST   /api/entitlements/users/{userId}/roles              # Assign role to user
  DELETE /api/entitlements/users/{userId}/roles/{roleId}     # Remove role from user
  GET    /api/entitlements/users/{userId}/roles/effective    # Get effective permissions

  # Role-based user queries
  GET    /api/entitlements/roles/{roleId}/users              # Get users with specific role
  GET    /api/entitlements/roles/{roleId}/users/active       # Get active users with role

  Usage:
  - Manage OneCMS users and their role assignments
  - Support user search for case assignment workflows
  - Query users by role for notification and routing

  5. Resource-Level Permission Endpoints (ABAC)

  (resource_permissions)

  # Direct Resource Permissions
  GET    /api/entitlements/resources/{resourceType}/{resourceId}/permissions     # Get resource permissions
  POST   /api/entitlements/resources/{resourceType}/{resourceId}/permissions     # Grant direct permission
  DELETE /api/entitlements/resources/{resourceType}/{resourceId}/permissions/{permissionId} # Revoke permission

  # User's resource permissions
  GET    /api/entitlements/users/{userId}/resource-permissions                   # All direct permissions for user
  GET    /api/entitlements/users/{userId}/resource-permissions?resource_type=case # Filter by resource type

  # Resource-specific queries
  GET    /api/entitlements/resources/case/{caseId}/users                        # Who has access to specific case
  GET    /api/entitlements/resources/workflow/{workflowId}/users                # Who has access to workflow

  # Bulk resource permissions
  POST   /api/entitlements/resources/permissions/bulk                           # Bulk grant permissions
  DELETE /api/entitlements/resources/permissions/bulk                           # Bulk revoke permissions

  Usage:
  - Grant specific users access to individual cases/workflows
  - Support case handoff scenarios
  - Temporary access for consultants or external reviewers

  6. Policy Management Endpoints

  (policies, match_conditions)

  # Policy Management
  GET    /api/entitlements/policies                          # List all policies
  POST   /api/entitlements/policies                          # Create new policy
  GET    /api/entitlements/policies/{policyId}               # Get policy details
  PUT    /api/entitlements/policies/{policyId}               # Update policy
  DELETE /api/entitlements/policies/{policyId}               # Delete policy
  PATCH  /api/entitlements/policies/{policyId}/activate      # Activate policy
  PATCH  /api/entitlements/policies/{policyId}/deactivate    # Deactivate policy

  # Policy testing and validation
  POST   /api/entitlements/policies/validate                 # Validate policy syntax
  POST   /api/entitlements/policies/{policyId}/test          # Test policy with sample data
  POST   /api/entitlements/policies/evaluate                 # Evaluate policies against request

  # Domain-specific policies
  GET    /api/entitlements/domains/{domainId}/policies       # Get domain policies
  POST   /api/entitlements/domains/{domainId}/policies       # Create domain policy

  # Policy conditions
  GET    /api/entitlements/policies/{policyId}/conditions    # Get policy conditions
  POST   /api/entitlements/policies/{policyId}/conditions    # Add condition to policy
  PUT    /api/entitlements/policies/{policyId}/conditions/{conditionId} # Update condition
  DELETE /api/entitlements/policies/{policyId}/conditions/{conditionId} # Delete condition

  Usage:
  - Define complex business rules that can't be handled by simple RBAC
  - Support conditional access based on time, location, case attributes
  - Test policies before deploying to production

  7. Hierarchical Structure Endpoints

  (entitlement_hierarchical_structures, entitlement_user_hierarchy_assignments)

  # Organizational Hierarchy
  GET    /api/entitlements/domains/{domainId}/hierarchy      # Get domain hierarchy
  POST   /api/entitlements/domains/{domainId}/hierarchy      # Create hierarchy node
  GET    /api/entitlements/hierarchy/{nodeId}                # Get node details
  PUT    /api/entitlements/hierarchy/{nodeId}                # Update node
  DELETE /api/entitlements/hierarchy/{nodeId}                # Delete node

  # Hierarchy navigation
  GET    /api/entitlements/hierarchy/{nodeId}/children       # Get child nodes
  GET    /api/entitlements/hierarchy/{nodeId}/parent         # Get parent node
  GET    /api/entitlements/hierarchy/{nodeId}/ancestors      # Get all ancestors
  GET    /api/entitlements/hierarchy/{nodeId}/descendants    # Get all descendants

  # User-Hierarchy Assignments
  GET    /api/entitlements/users/{userId}/hierarchy          # Get user's hierarchy assignments
  POST   /api/entitlements/users/{userId}/hierarchy          # Assign user to hierarchy node
  DELETE /api/entitlements/users/{userId}/hierarchy/{nodeId} # Remove user from node

  # Hierarchy-based queries
  GET    /api/entitlements/hierarchy/{nodeId}/users          # Get users in hierarchy node
  GET    /api/entitlements/hierarchy/{nodeId}/managers       # Get managers of node
  GET    /api/entitlements/hierarchy/{nodeId}/members        # Get members of node

  Usage:
  - Model organizational structure (departments, teams, regions)
  - Support hierarchical case routing and escalation
  - Enable "my team's cases" and "my department's cases" views

  8. Authorization Decision Endpoints

  (The core authorization functionality)

  # Authorization Checks
  POST   /api/entitlements/authorize                         # Main authorization endpoint
  POST   /api/entitlements/authorize/batch                   # Batch authorization requests
  POST   /api/entitlements/authorize/explain                 # Authorization with explanation

  # User context queries
  GET    /api/entitlements/users/{userId}/context            # Get user's full authorization context
  GET    /api/entitlements/users/{userId}/permissions/effective # Get effective permissions
  GET    /api/entitlements/users/{userId}/roles/effective    # Get effective roles

  # Resource access queries
  GET    /api/entitlements/resources/{resourceType}/{resourceId}/access/{userId} # Check specific access
  GET    /api/entitlements/users/{userId}/accessible-resources?type={resourceType} # Get accessible resources

  # Policy evaluation
  POST   /api/entitlements/policies/evaluate                 # Evaluate policies
  POST   /api/entitlements/derive-roles                      # Evaluate derived roles

  Usage:
  - OneCMS services call these for authorization decisions
  - Support debugging with detailed explanations
  - Enable UI to show/hide features based on permissions

  9. Audit and Reporting Endpoints

  (entitlement_audit_logs)

  # Audit Logs
  GET    /api/entitlements/audit/logs                        # Get audit logs
  GET    /api/entitlements/audit/logs/{logId}                # Get specific log entry
  POST   /api/entitlements/audit/logs/search                 # Search audit logs

  # Audit filtering and reporting
  GET    /api/entitlements/audit/logs?user_id={userId}       # User activity logs
  GET    /api/entitlements/audit/logs?resource_type=case     # Resource-specific logs
  GET    /api/entitlements/audit/logs?decision=DENY          # Failed access attempts
  GET    /api/entitlements/audit/logs?date_range={range}     # Time-based logs

  # Audit analytics
  GET    /api/entitlements/audit/stats                       # Authorization statistics
  GET    /api/entitlements/audit/users/{userId}/activity     # User activity summary
  GET    /api/entitlements/audit/resources/{resourceType}/access-patterns # Access patterns

  # Compliance reports
  GET    /api/entitlements/audit/compliance/access-review    # Access review report
  GET    /api/entitlements/audit/compliance/permissions-report # Permissions report
  POST   /api/entitlements/audit/export                      # Export audit data

  Usage:
  - Compliance reporting for security audits
  - Monitor unusual access patterns
  - User activity tracking for investigations

  10. Cache and System Management Endpoints

  # Cache Management
  DELETE /api/entitlements/cache                             # Clear all caches
  DELETE /api/entitlements/cache/users/{userId}              # Clear user-specific cache
  DELETE /api/entitlements/cache/policies                    # Clear policy cache
  GET    /api/entitlements/cache/stats                       # Get cache statistics

  # System Management
  GET    /api/entitlements/system/health                     # System health check
  GET    /api/entitlements/system/info                       # System information
  POST   /api/entitlements/system/engine/switch              # Switch authorization engine
  GET    /api/entitlements/system/engine/status              # Current engine status

  # Data Management
  POST   /api/entitlements/data/import                       # Import policies/users
  POST   /api/entitlements/data/export                       # Export configuration
  POST   /api/entitlements/data/migrate                      # Migrate from Cerbos
  POST   /api/entitlements/data/validate                     # Validate data integrity

  Usage:
  - Administrative functions for system maintenance
  - Support migration between authorization engines
  - Performance monitoring and troubleshooting

  11. Migration and Integration Endpoints

  # Cerbos Integration
  POST   /api/entitlements/cerbos/import                     # Import Cerbos policies
  POST   /api/entitlements/cerbos/sync                       # Sync with Cerbos
  GET    /api/entitlements/cerbos/compare                    # Compare DB vs Cerbos policies

  # Bulk Operations
  POST   /api/entitlements/bulk/users/import                 # Bulk user import
  POST   /api/entitlements/bulk/roles/assign                 # Bulk role assignments
  POST   /api/entitlements/bulk/permissions/grant            # Bulk permission grants

  # Templates and Presets
  GET    /api/entitlements/templates/roles                   # Get role templates
  POST   /api/entitlements/templates/roles/{templateId}/apply # Apply role template
  GET    /api/entitlements/presets/onecms                    # Get OneCMS preset configuration
  POST   /api/entitlements/presets/onecms/apply              # Apply OneCMS presets

  Usage:
  - Simplify initial setup and configuration
  - Support migration from existing authorization systems
  - Provide OneCMS-specific templates and presets

  Endpoint Priority for OneCMS Implementation

  Phase 1 - Core Authorization (MVP)

  1. POST /api/entitlements/authorize - Main authorization endpoint
  2. GET /api/entitlements/users/{userId}/context - User context loading
  3. POST /api/entitlements/users - User management
  4. POST /api/entitlements/domains/{domainId}/roles - Role management

  Phase 2 - Advanced Features

  5. POST /api/entitlements/resources/{resourceType}/{resourceId}/permissions - Direct permissions
  6. POST /api/entitlements/policies - Policy management
  7. GET /api/entitlements/audit/logs - Audit logging

  Phase 3 - Administration & Integration

  8. POST /api/entitlements/cerbos/import - Cerbos migration
  9. GET /api/entitlements/cache/stats - Performance monitoring
  10. POST /api/entitlements/presets/onecms/apply - OneCMS setup


  Key Benefits of This Design:

  1. ✅ Simple Toggle: authorization.engine.use-cerbos=false switches to database
  2. ✅ Abstract Pattern: Clean interfaces, swappable implementations
  3. ✅ Caffeine Cache: High-performance in-memory caching with stats
  4. ✅ Policy Translation: Automated Cerbos → Database policy conversion
  5. ✅ Multi-Tenant Ready: Domain-based isolation for SaaS deployments
  6. ✅ Complete RBAC/ABAC: Role, attribute, and policy-based access control
  7. ✅ Audit Trail: Comprehensive decision logging
  8. ✅ Performance: Sub-millisecond cache hits, optimized database queries

   This design provides a robust, configurable system that can run purely on database policies for multi-tenant environments while maintaining compatibility with Cerbos for complex scenarios.


  1. Implement all the 3 Phases
  2. Create migration scripts for the database schema
  3. Build the policy translation utility
  4. Add performance monitoring and metrics
  5. Create integration tests for both engines

 Endpoint Priority for OneCMS Implementation

  Phase 1 - Core Authorization (MVP)

  1. POST /api/entitlements/authorize - Main authorization endpoint
  2. GET /api/entitlements/users/{userId}/context - User context loading
  3. POST /api/entitlements/users - User management
  4. POST /api/entitlements/domains/{domainId}/roles - Role management

  Phase 2 - Advanced Features

  5. POST /api/entitlements/resources/{resourceType}/{resourceId}/permissions - Direct permissions
  6. POST /api/entitlements/policies - Policy management
  7. GET /api/entitlements/audit/logs - Audit logging

  Phase 3 - Administration & Integration

  8. POST /api/entitlements/cerbos/import - Cerbos migration
  9. GET /api/entitlements/cache/stats - Performance monitoring
  10. POST /api/entitlements/presets/onecms/apply - OneCMS setup