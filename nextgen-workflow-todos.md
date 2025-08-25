# NextGen Workflow - Entitlements Implementation TODOs

## Overview
This document tracks the implementation plan for the NextGen Workflow entitlements system using RBAC (Role-Based Access Control) and ABAC (Attribute-Based Access Control) with Cerbos policy engine.

## Current Status 🚀

### Infrastructure Status ✅
- **Docker Services**: Running
  - PostgreSQL 16: `localhost:5432` (nextgen_workflow database)
  - Cerbos 0.46.0: `localhost:3592` (gRPC) / `localhost:3593` (HTTP)
- **Database Schemas**: Created (entitlements, flowable, onecms)
- **Service Users**: Created with proper permissions
- **Liquibase Tables**: Ready for migrations

### Services Status
- **PostgreSQL**: ✅ Healthy and initialized
- **Cerbos**: ✅ Healthy and ready for policies
- **Liquibase Structure**: ✅ Prepared for entitlements migration

### Next Steps
Ready to implement Phase 2: RBAC Implementation with Liquibase migrations.

## Completed Tasks ✅

### 1. Database Infrastructure Setup
- [x] Verified Docker volume configuration for PostgreSQL persistence
- [x] Updated `init-db.sql` to remove UUID extensions
- [x] Added schema creation for `entitlements`, `flowable`, and `onecms`
- [x] Configured proper user permissions and cross-schema access

### 2. Database Schema Design
- [x] Created `entitlements-schema.sql` with proper naming conventions
- [x] Implemented sequences instead of UUIDs
- [x] Used entity-specific ID names (e.g., `user_id`, `role_id`)
- [x] Designed tables for:
  - Users and authentication
  - Departments (EO, ER, CSIS)
  - Roles (department-specific and system roles)
  - Permissions (RBAC)
  - Queues and workflow steps
  - User-role-department relationships
  - Session management
  - Audit logging

### 3. Documentation Updates
- [x] Updated CLAUDE.md with database naming conventions
- [x] Added department clarification (EO = Ethics Office)
- [x] Documented the proper ID naming pattern

## Pending Tasks 📋

### Phase 1: Infrastructure & RBAC Foundation
- [x] Start Docker containers (PostgreSQL and Cerbos) ✅
- [x] Initialize database with schemas and users ✅
- [ ] Create Liquibase migrations for entitlements schema
- [ ] Implement RBAC permission structure based on persona matrix

### Phase 2: RBAC Implementation (PostgreSQL)
- [ ] **Role-Permission Mapping**:
  - [ ] Create roles for all personas (EO Intake Analyst, ER Investigator, etc.)
  - [ ] Define permissions for workflow steps (task:create_case, task:approve_closures)
  - [ ] Map queue access permissions (queue:my-work-items:view, queue:all-open-cases:view)
  - [ ] Implement phase-based permissions (Intake, Investigation, Closure)

- [ ] **Database Tables** (via Liquibase):
  - [ ] Populate `roles` table with all personas from screenshot
  - [ ] Populate `permissions` table with workflow actions
  - [ ] Create `role_queue_access` mappings
  - [ ] Create `role_workflow_steps` mappings
  - [ ] Insert initial test users with role assignments

### Phase 3: ABAC Implementation (Cerbos Policies)
- [ ] **Derived Roles** (`/policies/derived_roles/one-cms.yaml`):
  - [ ] `assignee` - User assigned to case
  - [ ] `department_member` - User belongs to case's department
  - [ ] `queue_member` - User has access to specific queue
  - [ ] `phase_participant` - User can participate in current workflow phase

- [ ] **Resource Policies**:
  - [ ] **Case Resource** (`/policies/resources/case.yaml`):
    - [ ] View: assignee, department_member, or queue access
    - [ ] Edit: assignee in correct phase
    - [ ] Workflow actions: role + phase + department match
  - [ ] **Task Resource** (`/policies/resources/task.yaml`):
    - [ ] Claim: queue_member + correct role
    - [ ] Complete: task assignee + correct phase
    - [ ] Reassign: manager role in same department
  - [ ] **Queue Resource** (`/policies/resources/queue.yaml`):
    - [ ] View: queue_member
    - [ ] Process: queue_member + active role

### Phase 4: Service Integration
- [ ] **Entitlement Service Updates**:
  - [ ] Update JPA entities to match new schema naming conventions
  - [ ] Create AuthorizationService with Cerbos integration
  - [ ] Implement principal building (user + attributes)
  - [ ] Add department context to authorization checks

- [ ] **API Gateway Updates**:
  - [ ] Enhance HeaderUserExtractionFilter to include user attributes
  - [ ] Add department and queue information to X-User headers

- [ ] **Domain Service Integration**:
  - [ ] Update OneCMS service to call authorization checks
  - [ ] Add workflow step authorization
  - [ ] Implement queue-based access control

### Phase 5: Queue-Based Access Control
Based on persona matrix, implement three queue types:
- [ ] **My Work Items** - Personal tasks assigned to user
- [ ] **All Open Cases** - Department-wide visibility based on role
- [ ] **My Open Investigations** - Active investigations for user

- [ ] **Queue Access Rules**:
  - [ ] Intake Analysts: intake-related queues only
  - [ ] Investigators: investigation-related queues
  - [ ] Managers: all queues in their department
  - [ ] Cross-department access for multi-department cases

### Phase 6: Workflow Phase Security
- [ ] **Intake Phase**:
  - [ ] EO/ER/CSIS Intake Analysts can create/reject cases
  - [ ] Officers can be assigned cases
  - [ ] Department-specific intake workflows

- [ ] **Investigation Phase**:
  - [ ] Only investigators can conduct investigations
  - [ ] Managers can assign/reassign investigators
  - [ ] Cross-department collaboration rules

- [ ] **Closure Phase**:
  - [ ] Department heads approve closures (EO Head, ER Manager)
  - [ ] Special roles for retention decisions (CSIS Manager)
  - [ ] Multi-approval workflows for complex cases

### Phase 7: Advanced Features & Testing
- [ ] **Advanced Authorization**:
  - [ ] Time-based access (business hours, deadlines)
  - [ ] Delegation capabilities (temporary role assignment)
  - [ ] Escalation workflows (auto-assignment on delays)

- [ ] **Testing Strategy**:
  - [ ] Unit tests for each persona's permissions
  - [ ] Integration tests for full authorization flow
  - [ ] Scenario tests based on workflow matrix
  - [ ] Performance tests for Cerbos policy evaluation
  - [ ] Security penetration testing

- [ ] **Audit & Monitoring**:
  - [ ] Comprehensive audit trail for all authorization decisions
  - [ ] Real-time monitoring of access patterns
  - [ ] Policy violation detection and alerting

## Execution Order

1. **Database Setup** (Day 1)
   - Run updated `init-db.sql`
   - Execute `entitlements-schema.sql`
   - Verify schema creation and permissions

2. **Entitlement Service Development** (Day 2-3)
   - Update entities and repositories
   - Implement service layer changes
   - Test with sample data

3. **Cerbos Integration** (Day 3-4)
   - Write policy files
   - Test authorization rules
   - Integrate with entitlement service

4. **Integration Testing** (Day 5)
   - End-to-end testing
   - Performance testing
   - Security validation

## Key Design Decisions

### 1. Database Naming
- All IDs use entity-specific names (no generic `id`)
- Sequences start at meaningful values (e.g., user_id_seq starts at 1000)
- Boolean fields use `is_` or `has_` prefix
- Timestamps use `_at` suffix

### 2. Department-Based Roles
- Roles can be assigned globally or per department
- User can have different roles in different departments
- Workflow permissions are department-aware

### 3. Queue Management
- Three queue types: WORK_ITEMS, OPEN_CASES, INVESTIGATIONS
- Queue access is permission-based
- Users can have different access levels (VIEW, CLAIM, COMPLETE)

### 4. Workflow Permissions
- Workflow steps are mapped to roles
- Department context is considered for permissions
- Audit trail tracks all workflow actions

## Notes

- **No UUID Extensions**: Using sequences for all IDs
- **Department Correction**: EO = Ethics Office (not Equal Opportunity)
- **Session-Based Auth**: Moved away from JWT to session IDs
- **Cross-Service Access**: Services can read from other schemas but not write

## Next Steps

1. Review this plan with the team
2. Set up development environment with new schema
3. Begin implementation following the execution order
4. Regular testing and validation at each step