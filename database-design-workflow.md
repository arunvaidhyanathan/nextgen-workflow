# Database Design for Workflow Management System

## Overview

This document describes the database design for the NextGen Workflow Management System, focusing on the workflow engine, task management, and case routing capabilities. The design supports the OneCMS Unified Workflow (OneCMS_Nextgen_WF.bpmn20.xml) with multiple departments, roles, and queue-based task distribution.

## Database Architecture

### Core Schemas

1. **entitlements** - User management, roles, and permissions
2. **flowable** - Workflow engine data (Flowable tables + custom extensions)
3. **onecms** - Case management and business data

## Entity Relationship Diagram

```
┌─────────────────────┐       ┌─────────────────────┐       ┌─────────────────────┐
│   DEPARTMENTS       │       │   WORKFLOW_ROLES    │       │   USERS            │
├─────────────────────┤       ├─────────────────────┤       ├─────────────────────┤
│ department_id (PK)  │◄──────│ department_id (FK)  │       │ user_id (PK)       │
│ department_code     │       │ role_id (PK)        │       │ username           │
│ department_name     │       │ role_code           │◄──────│ email              │
│ parent_dept_id      │       │ role_name           │       │ first_name         │
│ is_active           │       │ role_type           │       │ last_name          │
│ created_at          │       │ queue_name          │       │ department_id (FK) │
│ metadata            │       │ permissions         │       │ is_active          │
└─────────────────────┘       │ created_at          │       │ created_at         │
                              └─────────────────────┘       └─────────────────────┘
                                        │                              │
                                        └──────────┬───────────────────┘
                                                   │
                                        ┌──────────▼───────────────────┐
                                        │   USER_WORKFLOW_ROLES        │
                                        ├──────────────────────────────┤
                                        │ user_role_id (PK)            │
                                        │ user_id (FK)                 │
                                        │ role_id (FK)                 │
                                        │ assigned_by                  │
                                        │ assigned_at                  │
                                        │ valid_from                   │
                                        │ valid_until                  │
                                        └──────────────────────────────┘
                                                   │
┌─────────────────────┐       ┌─────────────────────┐       ┌─────────────────────┐
│ WORKFLOW_DEFINITIONS│       │ PROCESS_INSTANCES   │       │   WORKFLOW_TASKS   │
├─────────────────────┤       ├─────────────────────┤       ├─────────────────────┤
│ workflow_id (PK)    │◄──────│ workflow_id (FK)    │       │ task_id (PK)       │
│ process_key         │       │ process_instance_id │◄──────│ process_instance_id│
│ process_name        │       │     (PK)            │       │     (FK)           │
│ version             │       │ case_id (FK)        │       │ task_definition_key│
│ bpmn_xml            │       │ business_key        │       │ task_name          │
│ deployment_id       │       │ started_by          │       │ queue_name         │
│ deployed_at         │       │ started_at          │       │ assignee_id        │
│ is_active           │       │ completed_at        │       │ candidate_groups   │
│ metadata            │       │ status              │       │ priority           │
└─────────────────────┘       │ variables           │       │ status             │
                              └─────────────────────┘       │ created_at         │
                                                            │ claimed_at         │
                                                            │ completed_at       │
                                                            │ due_date           │
                                                            │ task_data          │
                                                            └─────────────────────┘
                                                                      │
┌─────────────────────┐       ┌─────────────────────┐       ┌──────▼──────────────┐
│ WORKFLOW_QUEUES     │       │ QUEUE_ASSIGNMENTS   │       │ TASK_HISTORY       │
├─────────────────────┤       ├─────────────────────┤       ├─────────────────────┤
│ queue_id (PK)       │◄──────│ queue_id (FK)       │       │ history_id (PK)    │
│ queue_name          │       │ assignment_id (PK)  │       │ task_id (FK)       │
│ department_id (FK)  │       │ role_id (FK)        │       │ action_type        │
│ queue_type          │       │ priority_level      │       │ action_by          │
│ max_capacity        │       │ is_active           │       │ action_at          │
│ current_load        │       └─────────────────────┘       │ old_status         │
│ sla_minutes         │                                     │ new_status         │
│ is_active           │                                     │ comments           │
└─────────────────────┘                                     └─────────────────────┘
```

## Table Definitions

### Schema: entitlements

#### 1. DEPARTMENTS
Stores organizational departments involved in the workflow.

```sql
CREATE TABLE departments (
    department_id BIGSERIAL PRIMARY KEY,
    department_code VARCHAR(50) UNIQUE NOT NULL,
    department_name VARCHAR(100) NOT NULL,
    parent_dept_id BIGINT REFERENCES departments(department_id),
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_departments_code ON departments(department_code);
CREATE INDEX idx_departments_active ON departments(is_active);
```

#### 2. WORKFLOW_ROLES
Defines roles that can be assigned to users for workflow participation.

```sql
CREATE TABLE workflow_roles (
    role_id BIGSERIAL PRIMARY KEY,
    role_code VARCHAR(100) UNIQUE NOT NULL,
    role_name VARCHAR(200) NOT NULL,
    role_type VARCHAR(50) NOT NULL CHECK (role_type IN ('SYSTEM', 'DEPARTMENT', 'CUSTOM')),
    department_id BIGINT REFERENCES departments(department_id),
    queue_name VARCHAR(100),
    permissions JSONB DEFAULT '{}'::jsonb,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_workflow_roles_code ON workflow_roles(role_code);
CREATE INDEX idx_workflow_roles_dept ON workflow_roles(department_id);
CREATE INDEX idx_workflow_roles_queue ON workflow_roles(queue_name);
```

#### 3. USER_WORKFLOW_ROLES
Junction table linking users to their workflow roles.

```sql
CREATE TABLE user_workflow_roles (
    user_role_id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    role_id BIGINT NOT NULL REFERENCES workflow_roles(role_id),
    assigned_by VARCHAR(50) NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    valid_from TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    valid_until TIMESTAMP,
    is_primary BOOLEAN DEFAULT FALSE,
    metadata JSONB DEFAULT '{}'::jsonb,
    UNIQUE(user_id, role_id)
);

CREATE INDEX idx_user_workflow_roles_user ON user_workflow_roles(user_id);
CREATE INDEX idx_user_workflow_roles_role ON user_workflow_roles(role_id);
CREATE INDEX idx_user_workflow_roles_validity ON user_workflow_roles(valid_from, valid_until);
```

### Schema: flowable

#### 4. WORKFLOW_DEFINITIONS
Extended metadata for workflow process definitions.

```sql
CREATE TABLE workflow_definitions (
    workflow_id BIGSERIAL PRIMARY KEY,
    process_key VARCHAR(255) UNIQUE NOT NULL,
    process_name VARCHAR(500) NOT NULL,
    process_version INTEGER NOT NULL DEFAULT 1,
    business_app VARCHAR(100) NOT NULL,
    deployment_id VARCHAR(255),
    bpmn_xml TEXT,
    dmn_xml TEXT,
    deployed_at TIMESTAMP,
    deployed_by VARCHAR(50),
    is_active BOOLEAN DEFAULT FALSE,
    entry_criteria JSONB DEFAULT '{}'::jsonb,
    routing_rules JSONB DEFAULT '{}'::jsonb,
    sla_config JSONB DEFAULT '{}'::jsonb,
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(process_key, process_version)
);

CREATE INDEX idx_workflow_definitions_key ON workflow_definitions(process_key);
CREATE INDEX idx_workflow_definitions_app ON workflow_definitions(business_app);
CREATE INDEX idx_workflow_definitions_active ON workflow_definitions(is_active);
```

#### 5. PROCESS_INSTANCES
Tracks running workflow process instances.

```sql
CREATE TABLE process_instances (
    process_instance_id VARCHAR(64) PRIMARY KEY,
    workflow_id BIGINT NOT NULL REFERENCES workflow_definitions(workflow_id),
    case_id BIGINT,
    business_key VARCHAR(255),
    parent_process_id VARCHAR(64),
    super_process_id VARCHAR(64),
    started_by VARCHAR(50) NOT NULL,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    suspended_at TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    current_activity VARCHAR(255),
    variables JSONB DEFAULT '{}'::jsonb,
    context_data JSONB DEFAULT '{}'::jsonb,
    sla_due_date TIMESTAMP,
    priority INTEGER DEFAULT 50,
    tags TEXT[],
    metadata JSONB DEFAULT '{}'::jsonb,
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'SUSPENDED', 'TERMINATED', 'ERROR'))
);

CREATE INDEX idx_process_instances_workflow ON process_instances(workflow_id);
CREATE INDEX idx_process_instances_case ON process_instances(case_id);
CREATE INDEX idx_process_instances_business_key ON process_instances(business_key);
CREATE INDEX idx_process_instances_status ON process_instances(status);
CREATE INDEX idx_process_instances_started ON process_instances(started_at);
CREATE INDEX idx_process_instances_tags ON process_instances USING GIN(tags);
```

#### 6. WORKFLOW_TASKS
Queue-based task management for workflow user tasks.

```sql
CREATE TABLE workflow_tasks (
    task_id VARCHAR(64) PRIMARY KEY,
    process_instance_id VARCHAR(64) NOT NULL REFERENCES process_instances(process_instance_id),
    task_definition_key VARCHAR(255) NOT NULL,
    task_name VARCHAR(500) NOT NULL,
    task_type VARCHAR(50) NOT NULL DEFAULT 'USER_TASK',
    queue_name VARCHAR(100) NOT NULL,
    assignee_id VARCHAR(50),
    candidate_groups TEXT[],
    candidate_users TEXT[],
    priority INTEGER DEFAULT 50,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    form_key VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    claimed_at TIMESTAMP,
    claimed_by VARCHAR(50),
    completed_at TIMESTAMP,
    completed_by VARCHAR(50),
    due_date TIMESTAMP,
    follow_up_date TIMESTAMP,
    escalation_level INTEGER DEFAULT 0,
    escalated_at TIMESTAMP,
    task_data JSONB DEFAULT '{}'::jsonb,
    form_data JSONB DEFAULT '{}'::jsonb,
    outcome VARCHAR(100),
    outcome_reason TEXT,
    metadata JSONB DEFAULT '{}'::jsonb,
    CONSTRAINT chk_task_status CHECK (status IN ('OPEN', 'CLAIMED', 'COMPLETED', 'CANCELLED', 'DELEGATED', 'ESCALATED'))
);

CREATE INDEX idx_workflow_tasks_process ON workflow_tasks(process_instance_id);
CREATE INDEX idx_workflow_tasks_queue ON workflow_tasks(queue_name, status);
CREATE INDEX idx_workflow_tasks_assignee ON workflow_tasks(assignee_id, status);
CREATE INDEX idx_workflow_tasks_status ON workflow_tasks(status);
CREATE INDEX idx_workflow_tasks_priority ON workflow_tasks(priority DESC, created_at);
CREATE INDEX idx_workflow_tasks_due_date ON workflow_tasks(due_date) WHERE due_date IS NOT NULL;
CREATE INDEX idx_workflow_tasks_candidate_groups ON workflow_tasks USING GIN(candidate_groups);
```

#### 7. WORKFLOW_QUEUES
Defines task queues and their configurations.

```sql
CREATE TABLE workflow_queues (
    queue_id BIGSERIAL PRIMARY KEY,
    queue_name VARCHAR(100) UNIQUE NOT NULL,
    queue_type VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
    department_id BIGINT REFERENCES departments(department_id),
    description TEXT,
    max_capacity INTEGER DEFAULT 100,
    current_load INTEGER DEFAULT 0,
    sla_minutes INTEGER DEFAULT 480,
    escalation_config JSONB DEFAULT '{}'::jsonb,
    routing_rules JSONB DEFAULT '{}'::jsonb,
    priority_weights JSONB DEFAULT '{}'::jsonb,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB DEFAULT '{}'::jsonb,
    CONSTRAINT chk_queue_type CHECK (queue_type IN ('STANDARD', 'PRIORITY', 'SPECIALIZED', 'OVERFLOW'))
);

CREATE INDEX idx_workflow_queues_name ON workflow_queues(queue_name);
CREATE INDEX idx_workflow_queues_dept ON workflow_queues(department_id);
CREATE INDEX idx_workflow_queues_active ON workflow_queues(is_active);
```

#### 8. QUEUE_ASSIGNMENTS
Maps roles to queues for task distribution.

```sql
CREATE TABLE queue_assignments (
    assignment_id BIGSERIAL PRIMARY KEY,
    queue_id BIGINT NOT NULL REFERENCES workflow_queues(queue_id),
    role_id BIGINT NOT NULL REFERENCES workflow_roles(role_id),
    priority_level INTEGER DEFAULT 50,
    max_tasks_per_user INTEGER DEFAULT 10,
    auto_assign BOOLEAN DEFAULT FALSE,
    assignment_strategy VARCHAR(50) DEFAULT 'ROUND_ROBIN',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB DEFAULT '{}'::jsonb,
    UNIQUE(queue_id, role_id)
);

CREATE INDEX idx_queue_assignments_queue ON queue_assignments(queue_id);
CREATE INDEX idx_queue_assignments_role ON queue_assignments(role_id);
```

#### 9. TASK_HISTORY
Audit trail for all task actions.

```sql
CREATE TABLE task_history (
    history_id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL,
    process_instance_id VARCHAR(64) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    action_by VARCHAR(50) NOT NULL,
    action_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    old_status VARCHAR(50),
    new_status VARCHAR(50),
    old_assignee VARCHAR(50),
    new_assignee VARCHAR(50),
    delegate_to VARCHAR(50),
    escalate_to VARCHAR(50),
    comments TEXT,
    action_data JSONB DEFAULT '{}'::jsonb,
    client_info JSONB DEFAULT '{}'::jsonb,
    CONSTRAINT chk_action_type CHECK (action_type IN ('CREATE', 'CLAIM', 'UNCLAIM', 'COMPLETE', 'DELEGATE', 'ESCALATE', 'UPDATE', 'CANCEL', 'REASSIGN', 'COMMENT'))
);

CREATE INDEX idx_task_history_task ON task_history(task_id);
CREATE INDEX idx_task_history_process ON task_history(process_instance_id);
CREATE INDEX idx_task_history_action_by ON task_history(action_by);
CREATE INDEX idx_task_history_action_at ON task_history(action_at);
```

#### 10. WORKFLOW_VARIABLES
Stores process and task variables separately for better performance.

```sql
CREATE TABLE workflow_variables (
    variable_id BIGSERIAL PRIMARY KEY,
    process_instance_id VARCHAR(64),
    task_id VARCHAR(64),
    variable_name VARCHAR(255) NOT NULL,
    variable_type VARCHAR(50) NOT NULL,
    variable_value TEXT,
    variable_json JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    CONSTRAINT chk_variable_type CHECK (variable_type IN ('STRING', 'INTEGER', 'BOOLEAN', 'DATE', 'JSON', 'BINARY')),
    CONSTRAINT chk_variable_scope CHECK ((process_instance_id IS NOT NULL) OR (task_id IS NOT NULL))
);

CREATE INDEX idx_workflow_variables_process ON workflow_variables(process_instance_id) WHERE process_instance_id IS NOT NULL;
CREATE INDEX idx_workflow_variables_task ON workflow_variables(task_id) WHERE task_id IS NOT NULL;
CREATE INDEX idx_workflow_variables_name ON workflow_variables(variable_name);
```

### Schema: onecms

#### 11. CASE_WORKFLOW_MAPPING
Links cases to their workflow instances.

```sql
CREATE TABLE case_workflow_mapping (
    mapping_id BIGSERIAL PRIMARY KEY,
    case_id BIGINT NOT NULL,
    process_instance_id VARCHAR(64) NOT NULL REFERENCES process_instances(process_instance_id),
    workflow_type VARCHAR(100) NOT NULL,
    initiated_by VARCHAR(50) NOT NULL,
    initiated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    current_department VARCHAR(50),
    current_phase VARCHAR(100),
    expected_completion TIMESTAMP,
    actual_completion TIMESTAMP,
    routing_history JSONB DEFAULT '[]'::jsonb,
    metadata JSONB DEFAULT '{}'::jsonb,
    UNIQUE(case_id, process_instance_id)
);

CREATE INDEX idx_case_workflow_case ON case_workflow_mapping(case_id);
CREATE INDEX idx_case_workflow_process ON case_workflow_mapping(process_instance_id);
CREATE INDEX idx_case_workflow_type ON case_workflow_mapping(workflow_type);
```

## Views for Reporting

### 1. Active Tasks by Queue
```sql
CREATE VIEW v_active_tasks_by_queue AS
SELECT 
    q.queue_name,
    q.department_id,
    d.department_name,
    COUNT(CASE WHEN t.status = 'OPEN' THEN 1 END) as open_tasks,
    COUNT(CASE WHEN t.status = 'CLAIMED' THEN 1 END) as claimed_tasks,
    COUNT(CASE WHEN t.status = 'ESCALATED' THEN 1 END) as escalated_tasks,
    AVG(CASE WHEN t.status = 'COMPLETED' 
        THEN EXTRACT(EPOCH FROM (t.completed_at - t.created_at))/3600 
    END) as avg_completion_hours
FROM workflow_queues q
LEFT JOIN departments d ON q.department_id = d.department_id
LEFT JOIN workflow_tasks t ON q.queue_name = t.queue_name
WHERE q.is_active = TRUE
GROUP BY q.queue_name, q.department_id, d.department_name;
```

### 2. User Workload
```sql
CREATE VIEW v_user_workload AS
SELECT 
    u.user_id,
    u.username,
    u.first_name || ' ' || u.last_name as full_name,
    wr.role_code,
    wr.role_name,
    COUNT(CASE WHEN t.status = 'CLAIMED' THEN 1 END) as claimed_tasks,
    COUNT(CASE WHEN t.status = 'COMPLETED' 
          AND t.completed_at > CURRENT_TIMESTAMP - INTERVAL '7 days' THEN 1 END) as completed_last_week,
    AVG(CASE WHEN t.status = 'COMPLETED' 
        THEN EXTRACT(EPOCH FROM (t.completed_at - t.claimed_at))/3600 
    END) as avg_task_duration_hours
FROM users u
JOIN user_workflow_roles uwr ON u.user_id = uwr.user_id
JOIN workflow_roles wr ON uwr.role_id = wr.role_id
LEFT JOIN workflow_tasks t ON u.user_id = t.assignee_id
WHERE u.is_active = TRUE AND uwr.valid_until IS NULL OR uwr.valid_until > CURRENT_TIMESTAMP
GROUP BY u.user_id, u.username, u.first_name, u.last_name, wr.role_code, wr.role_name;
```

### 3. Process Performance Metrics
```sql
CREATE VIEW v_process_performance AS
SELECT 
    wd.process_key,
    wd.process_name,
    COUNT(pi.process_instance_id) as total_instances,
    COUNT(CASE WHEN pi.status = 'ACTIVE' THEN 1 END) as active_instances,
    COUNT(CASE WHEN pi.status = 'COMPLETED' THEN 1 END) as completed_instances,
    AVG(CASE WHEN pi.status = 'COMPLETED' 
        THEN EXTRACT(EPOCH FROM (pi.completed_at - pi.started_at))/3600 
    END) as avg_completion_hours,
    PERCENTILE_CONT(0.95) WITHIN GROUP (
        ORDER BY CASE WHEN pi.status = 'COMPLETED' 
        THEN EXTRACT(EPOCH FROM (pi.completed_at - pi.started_at))/3600 END
    ) as p95_completion_hours
FROM workflow_definitions wd
LEFT JOIN process_instances pi ON wd.workflow_id = pi.workflow_id
WHERE wd.is_active = TRUE
GROUP BY wd.process_key, wd.process_name;
```

## Indexes for Performance

```sql
-- Composite indexes for common queries
CREATE INDEX idx_workflow_tasks_queue_status_priority 
    ON workflow_tasks(queue_name, status, priority DESC, created_at);

CREATE INDEX idx_process_instances_workflow_status_started 
    ON process_instances(workflow_id, status, started_at DESC);

CREATE INDEX idx_task_history_task_action_at 
    ON task_history(task_id, action_at DESC);

-- Partial indexes for active records
CREATE INDEX idx_workflow_tasks_open_queue 
    ON workflow_tasks(queue_name, created_at) 
    WHERE status = 'OPEN';

CREATE INDEX idx_process_instances_active 
    ON process_instances(workflow_id, started_at) 
    WHERE status = 'ACTIVE';

-- Expression indexes
CREATE INDEX idx_workflow_tasks_overdue 
    ON workflow_tasks((due_date < CURRENT_TIMESTAMP)) 
    WHERE status IN ('OPEN', 'CLAIMED') AND due_date IS NOT NULL;
```

## Stored Procedures

### 1. Claim Next Available Task
```sql
CREATE OR REPLACE FUNCTION claim_next_task(
    p_user_id VARCHAR(50),
    p_queue_names TEXT[]
) RETURNS TABLE (
    task_id VARCHAR(64),
    task_name VARCHAR(500),
    priority INTEGER
) AS $$
DECLARE
    v_task_id VARCHAR(64);
BEGIN
    -- Select and lock the highest priority available task
    SELECT t.task_id INTO v_task_id
    FROM workflow_tasks t
    WHERE t.queue_name = ANY(p_queue_names)
      AND t.status = 'OPEN'
      AND (t.candidate_groups && (
          SELECT array_agg(wr.role_code)
          FROM user_workflow_roles uwr
          JOIN workflow_roles wr ON uwr.role_id = wr.role_id
          WHERE uwr.user_id = p_user_id
            AND (uwr.valid_until IS NULL OR uwr.valid_until > CURRENT_TIMESTAMP)
      ) OR t.candidate_users @> ARRAY[p_user_id])
    ORDER BY t.priority DESC, t.created_at
    LIMIT 1
    FOR UPDATE SKIP LOCKED;

    IF v_task_id IS NOT NULL THEN
        -- Update task status
        UPDATE workflow_tasks
        SET status = 'CLAIMED',
            assignee_id = p_user_id,
            claimed_at = CURRENT_TIMESTAMP,
            claimed_by = p_user_id
        WHERE task_id = v_task_id;

        -- Insert history record
        INSERT INTO task_history (
            task_id, process_instance_id, action_type, action_by,
            old_status, new_status
        )
        SELECT task_id, process_instance_id, 'CLAIM', p_user_id,
               'OPEN', 'CLAIMED'
        FROM workflow_tasks
        WHERE task_id = v_task_id;

        -- Return task details
        RETURN QUERY
        SELECT t.task_id, t.task_name, t.priority
        FROM workflow_tasks t
        WHERE t.task_id = v_task_id;
    END IF;
END;
$$ LANGUAGE plpgsql;
```

### 2. Escalate Overdue Tasks
```sql
CREATE OR REPLACE PROCEDURE escalate_overdue_tasks()
AS $$
DECLARE
    v_task RECORD;
BEGIN
    FOR v_task IN 
        SELECT t.task_id, t.queue_name, t.escalation_level,
               q.escalation_config
        FROM workflow_tasks t
        JOIN workflow_queues q ON t.queue_name = q.queue_name
        WHERE t.status IN ('OPEN', 'CLAIMED')
          AND t.due_date < CURRENT_TIMESTAMP
          AND t.escalation_level < 3
    LOOP
        -- Update task escalation
        UPDATE workflow_tasks
        SET escalation_level = escalation_level + 1,
            escalated_at = CURRENT_TIMESTAMP,
            status = 'ESCALATED'
        WHERE task_id = v_task.task_id;

        -- Insert history
        INSERT INTO task_history (
            task_id, process_instance_id, action_type, action_by,
            old_status, new_status, comments
        )
        SELECT task_id, process_instance_id, 'ESCALATE', 'SYSTEM',
               status, 'ESCALATED', 'Auto-escalated due to SLA breach'
        FROM workflow_tasks
        WHERE task_id = v_task.task_id;
    END LOOP;
END;
$$ LANGUAGE plpgsql;
```

## Security Considerations

1. **Row Level Security (RLS)**
```sql
-- Enable RLS on sensitive tables
ALTER TABLE workflow_tasks ENABLE ROW LEVEL SECURITY;

-- Policy for users to see only their tasks or tasks in their queues
CREATE POLICY task_access_policy ON workflow_tasks
    FOR SELECT
    USING (
        assignee_id = current_setting('app.current_user_id')::VARCHAR
        OR queue_name IN (
            SELECT wr.queue_name
            FROM user_workflow_roles uwr
            JOIN workflow_roles wr ON uwr.role_id = wr.role_id
            WHERE uwr.user_id = current_setting('app.current_user_id')::VARCHAR
              AND (uwr.valid_until IS NULL OR uwr.valid_until > CURRENT_TIMESTAMP)
        )
    );
```

2. **Audit Triggers**
```sql
-- Generic audit trigger function
CREATE OR REPLACE FUNCTION audit_trigger_function()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        NEW.updated_at = CURRENT_TIMESTAMP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply to all tables with updated_at
CREATE TRIGGER audit_trigger_workflow_definitions
    BEFORE UPDATE ON workflow_definitions
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();
```

## Migration Strategy

1. **Baseline Migration**: Create all tables, indexes, and constraints
2. **Data Migration**: Import existing workflow data from legacy systems
3. **View Creation**: Create reporting views
4. **Stored Procedures**: Deploy business logic procedures
5. **Security Setup**: Enable RLS and create policies
6. **Performance Tuning**: Analyze and optimize based on actual usage

## Maintenance Procedures

1. **Daily**: 
   - Vacuum analyze high-transaction tables
   - Check for overdue task escalations
   - Archive completed tasks older than retention period

2. **Weekly**:
   - Update table statistics
   - Review slow query logs
   - Check index usage and bloat

3. **Monthly**:
   - Full database backup
   - Review and optimize poorly performing queries
   - Update queue capacity based on workload analysis