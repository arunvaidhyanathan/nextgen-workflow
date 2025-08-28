-- Database Updates for EO_INTAKE_ANALYST Implementation
-- Execute these in order to add EO_INTAKE_ANALYST role and queue support

-- =======================================
-- 1. ADD EO_INTAKE_ANALYST WORKFLOW ROLE
-- =======================================

INSERT INTO workflow_roles (
    role_code, 
    role_name, 
    role_type, 
    queue_name, 
    permissions, 
    is_active, 
    created_at, 
    updated_at,
    metadata
) VALUES (
    'GROUP_EO_INTAKE_ANALYST',
    'EO Intake Analyst', 
    'SYSTEM', 
    'eo-intake-analyst-queue',
    '{"actions": ["create_case", "intake_review", "triage_case"]}'::jsonb,
    true, 
    CURRENT_TIMESTAMP, 
    CURRENT_TIMESTAMP,
    '{"description": "Ethics Office Intake Analyst role for initial case intake and creation", "department": "EO"}'::jsonb
);

-- =======================================
-- 2. ADD EO INTAKE ANALYST QUEUE
-- =======================================

INSERT INTO workflow_queues (
    queue_name,
    queue_type, 
    description,
    max_capacity,
    current_load,
    sla_minutes,
    escalation_config,
    routing_rules,
    priority_weights,
    is_active,
    created_at,
    updated_at,
    metadata
) VALUES (
    'eo-intake-analyst-queue',
    'STANDARD',
    'EO Intake Analyst Queue - Initial case intake and creation',
    50,
    0,
    240, -- 4 hours SLA
    '{"escalation_levels": [{"level": 1, "minutes": 240, "notify": ["supervisor"]}, {"level": 2, "minutes": 480, "notify": ["manager"]}]}'::jsonb,
    '{"auto_assign": false, "priority_based": true}'::jsonb,
    '{"high": 100, "medium": 50, "low": 25}'::jsonb,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    '{"department": "EO", "business_app": "OneCMS"}'::jsonb
);

-- =======================================
-- 3. CREATE QUEUE ASSIGNMENT MAPPING
-- =======================================

INSERT INTO queue_assignments (
    queue_id,
    role_id,
    priority_level,
    max_tasks_per_user,
    auto_assign,
    assignment_strategy,
    is_active,
    created_at,
    metadata
)
SELECT 
    wq.queue_id,
    wr.role_id,
    50, -- Medium priority
    10, -- Max 10 tasks per user
    false, -- Manual assignment
    'ROUND_ROBIN',
    true,
    CURRENT_TIMESTAMP,
    '{"created_for": "EO_INTAKE_ANALYST_implementation"}'::jsonb
FROM workflow_queues wq
CROSS JOIN workflow_roles wr
WHERE wq.queue_name = 'eo-intake-analyst-queue' 
  AND wr.role_code = 'GROUP_EO_INTAKE_ANALYST';

-- =======================================
-- 4. CREATE TEST USER WITH EO_INTAKE_ANALYST ROLE
-- =======================================

-- Insert test user (if not exists)
INSERT INTO users (user_id, username, email, first_name, last_name, is_active, created_at, updated_at)
VALUES ('eo.intake.test', 'eo.intake.test', 'eo.intake.test@company.com', 'EO', 'Intake Test', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (user_id) DO NOTHING;

-- Assign EO_INTAKE_ANALYST role to test user
INSERT INTO user_workflow_roles (
    user_id, 
    role_id, 
    assigned_by, 
    assigned_at, 
    valid_from, 
    valid_until,
    is_primary, 
    metadata
)
SELECT 
    'eo.intake.test',
    wr.role_id,
    'system',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL, -- No expiration
    true, -- Primary role
    '{"assignment_reason": "EO_INTAKE_ANALYST_implementation"}'::jsonb
FROM workflow_roles wr
WHERE wr.role_code = 'GROUP_EO_INTAKE_ANALYST'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- =======================================
-- 5. ADD EO DEPARTMENT (if not exists)
-- =======================================

INSERT INTO departments (department_code, department_name, description, is_active, created_at, updated_at, metadata)
VALUES (
    'EO',
    'Ethics Office', 
    'Ethics Office department responsible for case intake and ethical oversight',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    '{"type": "intake_department", "priority": "high"}'::jsonb
)
ON CONFLICT (department_code) DO UPDATE SET
    department_name = EXCLUDED.department_name,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP;

-- =======================================
-- 6. ASSIGN TEST USER TO EO DEPARTMENT
-- =======================================

-- Note: This assumes user_departments table exists
INSERT INTO user_departments (user_id, department_id, assigned_at, is_primary)
SELECT 
    'eo.intake.test',
    d.department_id,
    CURRENT_TIMESTAMP,
    true
FROM departments d
WHERE d.department_code = 'EO'
ON CONFLICT (user_id, department_id) DO NOTHING;

-- =======================================
-- 7. UPDATE EXISTING alice.intake USER (if exists)
-- =======================================

-- Add EO_INTAKE_ANALYST role to alice.intake for testing
INSERT INTO user_workflow_roles (
    user_id, 
    role_id, 
    assigned_by, 
    assigned_at, 
    valid_from, 
    is_primary, 
    metadata
)
SELECT 
    'alice.intake',
    wr.role_id,
    'system',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    false, -- Secondary role
    '{"assignment_reason": "EO_INTAKE_ANALYST_additional_role"}'::jsonb
FROM workflow_roles wr
WHERE wr.role_code = 'GROUP_EO_INTAKE_ANALYST'
  AND EXISTS (SELECT 1 FROM users WHERE user_id = 'alice.intake')
ON CONFLICT (user_id, role_id) DO NOTHING;

-- =======================================
-- 8. VERIFY THE SETUP
-- =======================================

-- Query to verify role creation
SELECT 
    'Role Created:' as check_type,
    role_code,
    role_name,
    queue_name,
    is_active
FROM workflow_roles 
WHERE role_code = 'GROUP_EO_INTAKE_ANALYST';

-- Query to verify queue creation
SELECT 
    'Queue Created:' as check_type,
    queue_name,
    queue_type,
    max_capacity,
    sla_minutes,
    is_active
FROM workflow_queues 
WHERE queue_name = 'eo-intake-analyst-queue';

-- Query to verify queue assignment
SELECT 
    'Queue Assignment:' as check_type,
    wq.queue_name,
    wr.role_code,
    qa.priority_level,
    qa.max_tasks_per_user,
    qa.is_active
FROM queue_assignments qa
JOIN workflow_queues wq ON qa.queue_id = wq.queue_id
JOIN workflow_roles wr ON qa.role_id = wr.role_id
WHERE wr.role_code = 'GROUP_EO_INTAKE_ANALYST';

-- Query to verify user role assignments
SELECT 
    'User Roles:' as check_type,
    uwr.user_id,
    wr.role_code,
    uwr.is_primary,
    uwr.assigned_at
FROM user_workflow_roles uwr
JOIN workflow_roles wr ON uwr.role_id = wr.role_id
WHERE wr.role_code = 'GROUP_EO_INTAKE_ANALYST';

-- Query to verify department assignments
SELECT 
    'User Departments:' as check_type,
    ud.user_id,
    d.department_code,
    d.department_name,
    ud.is_primary
FROM user_departments ud
JOIN departments d ON ud.department_id = d.department_id
WHERE ud.user_id IN (
    SELECT uwr.user_id 
    FROM user_workflow_roles uwr 
    JOIN workflow_roles wr ON uwr.role_id = wr.role_id 
    WHERE wr.role_code = 'GROUP_EO_INTAKE_ANALYST'
);

COMMIT;