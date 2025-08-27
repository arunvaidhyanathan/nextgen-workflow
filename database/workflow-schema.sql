-- =============================================================================
-- NextGen Workflow Management System - Database Schema
-- =============================================================================
-- Description: Complete database schema for the workflow management system
--              supporting OneCMS Unified Workflow with queue-based task management
-- Version: 1.0
-- Created: 2025-08-27
-- =============================================================================

-- =============================================================================
-- SCHEMA: entitlements - User management, roles, and permissions
-- =============================================================================

-- Departments table
CREATE TABLE IF NOT EXISTS entitlements.departments (
    department_id BIGSERIAL PRIMARY KEY,
    department_code VARCHAR(50) UNIQUE NOT NULL,
    department_name VARCHAR(100) NOT NULL,
    parent_dept_id BIGINT REFERENCES entitlements.departments(department_id),
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB DEFAULT '{}'::jsonb
);

-- Workflow roles table
CREATE TABLE IF NOT EXISTS entitlements.workflow_roles (
    role_id BIGSERIAL PRIMARY KEY,
    role_code VARCHAR(100) UNIQUE NOT NULL,
    role_name VARCHAR(200) NOT NULL,
    role_type VARCHAR(50) NOT NULL CHECK (role_type IN ('SYSTEM', 'DEPARTMENT', 'CUSTOM')),
    department_id BIGINT REFERENCES entitlements.departments(department_id),
    queue_name VARCHAR(100),
    permissions JSONB DEFAULT '{}'::jsonb,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB DEFAULT '{}'::jsonb
);

-- User workflow roles junction table
CREATE TABLE IF NOT EXISTS entitlements.user_workflow_roles (
    user_role_id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    role_id BIGINT NOT NULL REFERENCES entitlements.workflow_roles(role_id),
    assigned_by VARCHAR(50) NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    valid_from TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    valid_until TIMESTAMP,
    is_primary BOOLEAN DEFAULT FALSE,
    metadata JSONB DEFAULT '{}'::jsonb,
    UNIQUE(user_id, role_id)
);

-- =============================================================================
-- SCHEMA: flowable - Workflow engine data and custom extensions
-- =============================================================================

-- Workflow definitions with extended metadata
CREATE TABLE IF NOT EXISTS flowable.workflow_definitions (
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

-- Process instances tracking
CREATE TABLE IF NOT EXISTS flowable.process_instances (
    process_instance_id VARCHAR(64) PRIMARY KEY,
    workflow_id BIGINT NOT NULL REFERENCES flowable.workflow_definitions(workflow_id),
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
    CONSTRAINT chk_process_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'SUSPENDED', 'TERMINATED', 'ERROR'))
);

-- Workflow task queues configuration
CREATE TABLE IF NOT EXISTS flowable.workflow_queues (
    queue_id BIGSERIAL PRIMARY KEY,
    queue_name VARCHAR(100) UNIQUE NOT NULL,
    queue_type VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
    department_id BIGINT REFERENCES entitlements.departments(department_id),
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

-- Queue role assignments
CREATE TABLE IF NOT EXISTS flowable.queue_assignments (
    assignment_id BIGSERIAL PRIMARY KEY,
    queue_id BIGINT NOT NULL REFERENCES flowable.workflow_queues(queue_id),
    role_id BIGINT NOT NULL REFERENCES entitlements.workflow_roles(role_id),
    priority_level INTEGER DEFAULT 50,
    max_tasks_per_user INTEGER DEFAULT 10,
    auto_assign BOOLEAN DEFAULT FALSE,
    assignment_strategy VARCHAR(50) DEFAULT 'ROUND_ROBIN',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB DEFAULT '{}'::jsonb,
    UNIQUE(queue_id, role_id)
);

-- Workflow tasks with queue management
CREATE TABLE IF NOT EXISTS flowable.workflow_tasks (
    task_id VARCHAR(64) PRIMARY KEY,
    process_instance_id VARCHAR(64) NOT NULL REFERENCES flowable.process_instances(process_instance_id),
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

-- Task history for audit trail
CREATE TABLE IF NOT EXISTS flowable.task_history (
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

-- Workflow variables storage
CREATE TABLE IF NOT EXISTS flowable.workflow_variables (
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

-- =============================================================================
-- SCHEMA: onecms - Case management and business data
-- =============================================================================

-- Case workflow mapping
CREATE TABLE IF NOT EXISTS onecms.case_workflow_mapping (
    mapping_id BIGSERIAL PRIMARY KEY,
    case_id BIGINT NOT NULL,
    process_instance_id VARCHAR(64) NOT NULL REFERENCES flowable.process_instances(process_instance_id),
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

-- =============================================================================
-- INDEXES FOR PERFORMANCE OPTIMIZATION
-- =============================================================================

-- Departments indexes
CREATE INDEX IF NOT EXISTS idx_departments_code ON entitlements.departments(department_code);
CREATE INDEX IF NOT EXISTS idx_departments_active ON entitlements.departments(is_active);

-- Workflow roles indexes
CREATE INDEX IF NOT EXISTS idx_workflow_roles_code ON entitlements.workflow_roles(role_code);
CREATE INDEX IF NOT EXISTS idx_workflow_roles_dept ON entitlements.workflow_roles(department_id);
CREATE INDEX IF NOT EXISTS idx_workflow_roles_queue ON entitlements.workflow_roles(queue_name);

-- User workflow roles indexes
CREATE INDEX IF NOT EXISTS idx_user_workflow_roles_user ON entitlements.user_workflow_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_workflow_roles_role ON entitlements.user_workflow_roles(role_id);
CREATE INDEX IF NOT EXISTS idx_user_workflow_roles_validity ON entitlements.user_workflow_roles(valid_from, valid_until);

-- Workflow definitions indexes
CREATE INDEX IF NOT EXISTS idx_workflow_definitions_key ON flowable.workflow_definitions(process_key);
CREATE INDEX IF NOT EXISTS idx_workflow_definitions_app ON flowable.workflow_definitions(business_app);
CREATE INDEX IF NOT EXISTS idx_workflow_definitions_active ON flowable.workflow_definitions(is_active);

-- Process instances indexes
CREATE INDEX IF NOT EXISTS idx_process_instances_workflow ON flowable.process_instances(workflow_id);
CREATE INDEX IF NOT EXISTS idx_process_instances_case ON flowable.process_instances(case_id);
CREATE INDEX IF NOT EXISTS idx_process_instances_business_key ON flowable.process_instances(business_key);
CREATE INDEX IF NOT EXISTS idx_process_instances_status ON flowable.process_instances(status);
CREATE INDEX IF NOT EXISTS idx_process_instances_started ON flowable.process_instances(started_at);
CREATE INDEX IF NOT EXISTS idx_process_instances_tags ON flowable.process_instances USING GIN(tags);

-- Workflow queues indexes
CREATE INDEX IF NOT EXISTS idx_workflow_queues_name ON flowable.workflow_queues(queue_name);
CREATE INDEX IF NOT EXISTS idx_workflow_queues_dept ON flowable.workflow_queues(department_id);
CREATE INDEX IF NOT EXISTS idx_workflow_queues_active ON flowable.workflow_queues(is_active);

-- Queue assignments indexes
CREATE INDEX IF NOT EXISTS idx_queue_assignments_queue ON flowable.queue_assignments(queue_id);
CREATE INDEX IF NOT EXISTS idx_queue_assignments_role ON flowable.queue_assignments(role_id);

-- Workflow tasks indexes
CREATE INDEX IF NOT EXISTS idx_workflow_tasks_process ON flowable.workflow_tasks(process_instance_id);
CREATE INDEX IF NOT EXISTS idx_workflow_tasks_queue ON flowable.workflow_tasks(queue_name, status);
CREATE INDEX IF NOT EXISTS idx_workflow_tasks_assignee ON flowable.workflow_tasks(assignee_id, status);
CREATE INDEX IF NOT EXISTS idx_workflow_tasks_status ON flowable.workflow_tasks(status);
CREATE INDEX IF NOT EXISTS idx_workflow_tasks_priority ON flowable.workflow_tasks(priority DESC, created_at);
CREATE INDEX IF NOT EXISTS idx_workflow_tasks_due_date ON flowable.workflow_tasks(due_date) WHERE due_date IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_workflow_tasks_candidate_groups ON flowable.workflow_tasks USING GIN(candidate_groups);

-- Composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_workflow_tasks_queue_status_priority 
    ON flowable.workflow_tasks(queue_name, status, priority DESC, created_at);

CREATE INDEX IF NOT EXISTS idx_process_instances_workflow_status_started 
    ON flowable.process_instances(workflow_id, status, started_at DESC);

-- Task history indexes
CREATE INDEX IF NOT EXISTS idx_task_history_task ON flowable.task_history(task_id);
CREATE INDEX IF NOT EXISTS idx_task_history_process ON flowable.task_history(process_instance_id);
CREATE INDEX IF NOT EXISTS idx_task_history_action_by ON flowable.task_history(action_by);
CREATE INDEX IF NOT EXISTS idx_task_history_action_at ON flowable.task_history(action_at);
CREATE INDEX IF NOT EXISTS idx_task_history_task_action_at ON flowable.task_history(task_id, action_at DESC);

-- Workflow variables indexes
CREATE INDEX IF NOT EXISTS idx_workflow_variables_process ON flowable.workflow_variables(process_instance_id) WHERE process_instance_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_workflow_variables_task ON flowable.workflow_variables(task_id) WHERE task_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_workflow_variables_name ON flowable.workflow_variables(variable_name);

-- Case workflow mapping indexes
CREATE INDEX IF NOT EXISTS idx_case_workflow_case ON onecms.case_workflow_mapping(case_id);
CREATE INDEX IF NOT EXISTS idx_case_workflow_process ON onecms.case_workflow_mapping(process_instance_id);
CREATE INDEX IF NOT EXISTS idx_case_workflow_type ON onecms.case_workflow_mapping(workflow_type);

-- Partial indexes for performance on active records
CREATE INDEX IF NOT EXISTS idx_workflow_tasks_open_queue 
    ON flowable.workflow_tasks(queue_name, created_at) 
    WHERE status = 'OPEN';

CREATE INDEX IF NOT EXISTS idx_process_instances_active 
    ON flowable.process_instances(workflow_id, started_at) 
    WHERE status = 'ACTIVE';

-- Expression indexes
CREATE INDEX IF NOT EXISTS idx_workflow_tasks_overdue 
    ON flowable.workflow_tasks((due_date < CURRENT_TIMESTAMP)) 
    WHERE status IN ('OPEN', 'CLAIMED') AND due_date IS NOT NULL;

-- =============================================================================
-- INITIAL DATA SEEDING
-- =============================================================================

-- Insert default departments
INSERT INTO entitlements.departments (department_code, department_name, description) VALUES 
('EO', 'Ethics Office', 'Ethics Office - Central intake and routing'),
('CSIS', 'Corporate Security & Information Security', 'Corporate Security and Information Security department'),
('ER', 'Employee Relations', 'Employee Relations department'),
('LEGAL', 'Legal Department', 'Legal Affairs department'),
('INVESTIGATION', 'Investigation Unit', 'Investigation execution unit')
ON CONFLICT (department_code) DO NOTHING;

-- Insert workflow roles based on OneCMS_Nextgen_WF.bpmn20.xml
INSERT INTO entitlements.workflow_roles (role_code, role_name, role_type, department_id, queue_name) VALUES 
-- Ethics Office roles
('GROUP_EO_HEAD', 'Ethics Office Head', 'DEPARTMENT', 
 (SELECT department_id FROM entitlements.departments WHERE department_code = 'EO'), 
 'eo-head-queue'),
('GROUP_EO_OFFICER', 'Ethics Office Officer', 'DEPARTMENT', 
 (SELECT department_id FROM entitlements.departments WHERE department_code = 'EO'), 
 'eo-officer-queue'),
-- CSIS roles
('GROUP_CSIS_INTAKE_MANAGER', 'CSIS Intake Manager', 'DEPARTMENT', 
 (SELECT department_id FROM entitlements.departments WHERE department_code = 'CSIS'), 
 'csis-intake-manager-queue'),
('GROUP_CSIS_INTAKE_ANALYST', 'CSIS Intake Analyst', 'DEPARTMENT', 
 (SELECT department_id FROM entitlements.departments WHERE department_code = 'CSIS'), 
 'csis-intake-analyst-queue'),
-- Employee Relations roles
('GROUP_ER_INTAKE_ANALYST', 'ER Intake Analyst', 'DEPARTMENT', 
 (SELECT department_id FROM entitlements.departments WHERE department_code = 'ER'), 
 'er-intake-analyst-queue'),
-- Legal Department roles
('GROUP_LEGAL_INTAKE_ANALYST', 'Legal Intake Analyst', 'DEPARTMENT', 
 (SELECT department_id FROM entitlements.departments WHERE department_code = 'LEGAL'), 
 'legal-intake-analyst-queue'),
-- Investigation Unit roles
('GROUP_INVESTIGATION_MANAGER', 'Investigation Manager', 'DEPARTMENT', 
 (SELECT department_id FROM entitlements.departments WHERE department_code = 'INVESTIGATION'), 
 'investigation-manager-queue'),
('GROUP_INVESTIGATOR', 'Investigator', 'DEPARTMENT', 
 (SELECT department_id FROM entitlements.departments WHERE department_code = 'INVESTIGATION'), 
 'investigator-queue')
ON CONFLICT (role_code) DO NOTHING;

-- Insert workflow queues
INSERT INTO flowable.workflow_queues (queue_name, queue_type, department_id, description, sla_minutes) VALUES 
('eo-head-queue', 'STANDARD', 
 (SELECT department_id FROM entitlements.departments WHERE department_code = 'EO'),
 'Queue for Ethics Office Head reviews', 240),
('eo-officer-queue', 'STANDARD', 
 (SELECT department_id FROM entitlements.departments WHERE department_code = 'EO'),
 'Queue for Ethics Office Officer triage', 480),
('csis-intake-manager-queue', 'STANDARD', 
 (SELECT department_id FROM entitlements.departments WHERE department_code = 'CSIS'),
 'Queue for CSIS Intake Manager reviews', 480),
('csis-intake-analyst-queue', 'STANDARD', 
 (SELECT department_id FROM entitlements.departments WHERE department_code = 'CSIS'),
 'Queue for CSIS Intake Analyst tasks', 720),
('er-intake-analyst-queue', 'STANDARD', 
 (SELECT department_id FROM entitlements.departments WHERE department_code = 'ER'),
 'Queue for Employee Relations Intake', 480),
('legal-intake-analyst-queue', 'STANDARD', 
 (SELECT department_id FROM entitlements.departments WHERE department_code = 'LEGAL'),
 'Queue for Legal Department Intake', 480),
('investigation-manager-queue', 'PRIORITY', 
 (SELECT department_id FROM entitlements.departments WHERE department_code = 'INVESTIGATION'),
 'Queue for Investigation Manager assignments', 240),
('investigator-queue', 'STANDARD', 
 (SELECT department_id FROM entitlements.departments WHERE department_code = 'INVESTIGATION'),
 'Queue for Investigator tasks', 2400)
ON CONFLICT (queue_name) DO NOTHING;

-- Insert queue assignments (role to queue mappings)
INSERT INTO flowable.queue_assignments (queue_id, role_id, priority_level, auto_assign) VALUES
-- EO assignments
((SELECT queue_id FROM flowable.workflow_queues WHERE queue_name = 'eo-head-queue'),
 (SELECT role_id FROM entitlements.workflow_roles WHERE role_code = 'GROUP_EO_HEAD'), 50, false),
((SELECT queue_id FROM flowable.workflow_queues WHERE queue_name = 'eo-officer-queue'),
 (SELECT role_id FROM entitlements.workflow_roles WHERE role_code = 'GROUP_EO_OFFICER'), 50, false),
-- CSIS assignments
((SELECT queue_id FROM flowable.workflow_queues WHERE queue_name = 'csis-intake-manager-queue'),
 (SELECT role_id FROM entitlements.workflow_roles WHERE role_code = 'GROUP_CSIS_INTAKE_MANAGER'), 50, false),
((SELECT queue_id FROM flowable.workflow_queues WHERE queue_name = 'csis-intake-analyst-queue'),
 (SELECT role_id FROM entitlements.workflow_roles WHERE role_code = 'GROUP_CSIS_INTAKE_ANALYST'), 50, false),
-- ER assignments
((SELECT queue_id FROM flowable.workflow_queues WHERE queue_name = 'er-intake-analyst-queue'),
 (SELECT role_id FROM entitlements.workflow_roles WHERE role_code = 'GROUP_ER_INTAKE_ANALYST'), 50, false),
-- Legal assignments
((SELECT queue_id FROM flowable.workflow_queues WHERE queue_name = 'legal-intake-analyst-queue'),
 (SELECT role_id FROM entitlements.workflow_roles WHERE role_code = 'GROUP_LEGAL_INTAKE_ANALYST'), 50, false),
-- Investigation assignments
((SELECT queue_id FROM flowable.workflow_queues WHERE queue_name = 'investigation-manager-queue'),
 (SELECT role_id FROM entitlements.workflow_roles WHERE role_code = 'GROUP_INVESTIGATION_MANAGER'), 50, false),
((SELECT queue_id FROM flowable.workflow_queues WHERE queue_name = 'investigator-queue'),
 (SELECT role_id FROM entitlements.workflow_roles WHERE role_code = 'GROUP_INVESTIGATOR'), 50, false)
ON CONFLICT (queue_id, role_id) DO NOTHING;

-- =============================================================================
-- COMMENTS AND DOCUMENTATION
-- =============================================================================

COMMENT ON SCHEMA entitlements IS 'User management, departments, roles, and permissions';
COMMENT ON SCHEMA flowable IS 'Workflow engine data with custom extensions for queue management';
COMMENT ON SCHEMA onecms IS 'Case management business data and workflow integration';

COMMENT ON TABLE entitlements.departments IS 'Organizational departments participating in workflows';
COMMENT ON TABLE entitlements.workflow_roles IS 'Roles that can be assigned to users for workflow participation';
COMMENT ON TABLE entitlements.user_workflow_roles IS 'Junction table mapping users to their workflow roles';

COMMENT ON TABLE flowable.workflow_definitions IS 'Extended metadata for BPMN process definitions';
COMMENT ON TABLE flowable.process_instances IS 'Active and historical process instances with context data';
COMMENT ON TABLE flowable.workflow_queues IS 'Task queues configuration and capacity management';
COMMENT ON TABLE flowable.queue_assignments IS 'Mapping between roles and queues for task distribution';
COMMENT ON TABLE flowable.workflow_tasks IS 'Queue-based task management with priority and SLA support';
COMMENT ON TABLE flowable.task_history IS 'Complete audit trail of all task actions and state changes';
COMMENT ON TABLE flowable.workflow_variables IS 'Process and task variables with optimized storage';

COMMENT ON TABLE onecms.case_workflow_mapping IS 'Links OneCMS cases to their workflow process instances';

-- =============================================================================
-- SUCCESS MESSAGE
-- =============================================================================

DO $$
BEGIN
    RAISE NOTICE 'NextGen Workflow Database Schema created successfully!';
    RAISE NOTICE 'Schemas: entitlements, flowable, onecms';
    RAISE NOTICE 'Tables created: % total', 
        (SELECT count(*) FROM information_schema.tables 
         WHERE table_schema IN ('entitlements', 'flowable', 'onecms'));
    RAISE NOTICE 'Indexes created for performance optimization';
    RAISE NOTICE 'Initial data seeded for departments, roles, queues, and assignments';
END $$;