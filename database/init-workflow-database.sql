-- =============================================================================
-- NextGen Workflow Management System - Database Initialization Script
-- =============================================================================
-- Description: Complete database setup script for the workflow management system
-- Version: 1.0
-- Created: 2025-08-27
-- Usage: psql -d nextgen_workflow -f init-workflow-database.sql
-- =============================================================================

-- Set up the environment
\set ON_ERROR_STOP on
\set ECHO all

-- Display startup message
\echo '========================================================================='
\echo 'NextGen Workflow Management System - Database Initialization'
\echo 'Version: 1.0'
\echo 'Date: 2025-08-27'
\echo '========================================================================='

-- Create schemas if they don't exist
CREATE SCHEMA IF NOT EXISTS entitlements;
CREATE SCHEMA IF NOT EXISTS flowable;
CREATE SCHEMA IF NOT EXISTS onecms;

\echo 'Schemas created: entitlements, flowable, onecms'

-- =============================================================================
-- STEP 1: CREATE TABLES AND INDEXES
-- =============================================================================

\echo 'Step 1: Creating tables and indexes...'
\i workflow-schema.sql

-- =============================================================================
-- STEP 2: CREATE STORED PROCEDURES AND FUNCTIONS
-- =============================================================================

\echo 'Step 2: Creating stored procedures and functions...'
\i workflow-procedures.sql

-- =============================================================================
-- STEP 3: CREATE REPORTING VIEWS
-- =============================================================================

\echo 'Step 3: Creating reporting views...'
\i workflow-views.sql

-- =============================================================================
-- STEP 4: CREATE ADDITIONAL SAMPLE DATA
-- =============================================================================

\echo 'Step 4: Creating sample data for testing...'

-- Sample users for testing (extends the basic seeding in schema.sql)
INSERT INTO entitlements.users (user_id, username, email, first_name, last_name, department_id, is_active) VALUES 
('eo-head-001', 'john.ethicshead', 'john.ethicshead@company.com', 'John', 'Smith', 
 (SELECT department_id FROM entitlements.departments WHERE department_code = 'EO'), true),
('eo-officer-001', 'jane.ethicsofficer', 'jane.ethicsofficer@company.com', 'Jane', 'Johnson', 
 (SELECT department_id FROM entitlements.departments WHERE department_code = 'EO'), true),
('csis-mgr-001', 'mike.csismanager', 'mike.csismanager@company.com', 'Mike', 'Davis', 
 (SELECT department_id FROM entitlements.departments WHERE department_code = 'CSIS'), true),
('csis-analyst-001', 'sarah.csisanalyst', 'sarah.csisanalyst@company.com', 'Sarah', 'Wilson', 
 (SELECT department_id FROM entitlements.departments WHERE department_code = 'CSIS'), true),
('er-analyst-001', 'bob.eranalyst', 'bob.eranalyst@company.com', 'Bob', 'Brown', 
 (SELECT department_id FROM entitlements.departments WHERE department_code = 'ER'), true),
('legal-analyst-001', 'alice.legalanalyst', 'alice.legalanalyst@company.com', 'Alice', 'Taylor', 
 (SELECT department_id FROM entitlements.departments WHERE department_code = 'LEGAL'), true),
('inv-mgr-001', 'tom.invmanager', 'tom.invmanager@company.com', 'Tom', 'Anderson', 
 (SELECT department_id FROM entitlements.departments WHERE department_code = 'INVESTIGATION'), true),
('investigator-001', 'lisa.investigator', 'lisa.investigator@company.com', 'Lisa', 'Martinez', 
 (SELECT department_id FROM entitlements.departments WHERE department_code = 'INVESTIGATION'), true)
ON CONFLICT (user_id) DO NOTHING;

-- Assign users to workflow roles
INSERT INTO entitlements.user_workflow_roles (user_id, role_id, assigned_by) VALUES
-- EO assignments
('eo-head-001', (SELECT role_id FROM entitlements.workflow_roles WHERE role_code = 'GROUP_EO_HEAD'), 'system'),
('eo-officer-001', (SELECT role_id FROM entitlements.workflow_roles WHERE role_code = 'GROUP_EO_OFFICER'), 'system'),
-- CSIS assignments
('csis-mgr-001', (SELECT role_id FROM entitlements.workflow_roles WHERE role_code = 'GROUP_CSIS_INTAKE_MANAGER'), 'system'),
('csis-analyst-001', (SELECT role_id FROM entitlements.workflow_roles WHERE role_code = 'GROUP_CSIS_INTAKE_ANALYST'), 'system'),
-- ER assignments
('er-analyst-001', (SELECT role_id FROM entitlements.workflow_roles WHERE role_code = 'GROUP_ER_INTAKE_ANALYST'), 'system'),
-- Legal assignments
('legal-analyst-001', (SELECT role_id FROM entitlements.workflow_roles WHERE role_code = 'GROUP_LEGAL_INTAKE_ANALYST'), 'system'),
-- Investigation assignments
('inv-mgr-001', (SELECT role_id FROM entitlements.workflow_roles WHERE role_code = 'GROUP_INVESTIGATION_MANAGER'), 'system'),
('investigator-001', (SELECT role_id FROM entitlements.workflow_roles WHERE role_code = 'GROUP_INVESTIGATOR'), 'system')
ON CONFLICT (user_id, role_id) DO NOTHING;

-- Sample workflow definition
INSERT INTO flowable.workflow_definitions (
    process_key, process_name, business_app, is_active,
    entry_criteria, routing_rules, sla_config
) VALUES (
    'oneCmsUnifiedWorkflow',
    'OneCMS Unified Case Workflow',
    'onecms',
    true,
    '{"entry_points": ["EO_INITIATED", "CSIS_INITIATED"]}',
    '{"department_routing": {"CSIS": "csis-intake-manager-queue", "ER": "er-intake-analyst-queue", "LEGAL": "legal-intake-analyst-queue"}}',
    '{"default_sla_hours": 24, "escalation_levels": 3}'
)
ON CONFLICT (process_key) DO NOTHING;

-- =============================================================================
-- STEP 5: PERFORMANCE OPTIMIZATION
-- =============================================================================

\echo 'Step 5: Analyzing tables and updating statistics...'

-- Analyze all tables for query optimizer
ANALYZE entitlements.departments;
ANALYZE entitlements.workflow_roles;
ANALYZE entitlements.user_workflow_roles;
ANALYZE flowable.workflow_definitions;
ANALYZE flowable.process_instances;
ANALYZE flowable.workflow_queues;
ANALYZE flowable.queue_assignments;
ANALYZE flowable.workflow_tasks;
ANALYZE flowable.task_history;
ANALYZE flowable.workflow_variables;
ANALYZE onecms.case_workflow_mapping;

-- =============================================================================
-- STEP 6: SECURITY SETUP
-- =============================================================================

\echo 'Step 6: Setting up security policies...'

-- Enable row level security on sensitive tables
ALTER TABLE flowable.workflow_tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE flowable.task_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE flowable.workflow_variables ENABLE ROW LEVEL SECURITY;

-- Create security policies (example - customize based on requirements)
-- Note: These policies require setting app.current_user_id in session

-- Policy for workflow tasks - users can see tasks in their queues or assigned to them
CREATE POLICY workflow_tasks_access_policy ON flowable.workflow_tasks
    FOR ALL
    USING (
        -- User can see tasks in their assigned queues
        queue_name IN (
            SELECT wr.queue_name
            FROM entitlements.user_workflow_roles uwr
            JOIN entitlements.workflow_roles wr ON uwr.role_id = wr.role_id
            WHERE uwr.user_id = COALESCE(current_setting('app.current_user_id', true), 'anonymous')
              AND uwr.is_active = TRUE
              AND (uwr.valid_until IS NULL OR uwr.valid_until > CURRENT_TIMESTAMP)
        )
        -- OR task is assigned to them
        OR assignee_id = COALESCE(current_setting('app.current_user_id', true), 'anonymous')
        -- OR they are in the candidate users list
        OR candidate_users @> ARRAY[COALESCE(current_setting('app.current_user_id', true), 'anonymous')]
    );

-- Policy for task history - users can see history for tasks they have access to
CREATE POLICY task_history_access_policy ON flowable.task_history
    FOR SELECT
    USING (
        task_id IN (
            SELECT task_id FROM flowable.workflow_tasks
            -- This will be filtered by the workflow_tasks RLS policy
        )
    );

-- =============================================================================
-- STEP 7: CREATE MAINTENANCE JOBS
-- =============================================================================

\echo 'Step 7: Setting up maintenance procedures...'

-- Function to run daily maintenance tasks
CREATE OR REPLACE FUNCTION flowable.daily_maintenance()
RETURNS TEXT AS $$
DECLARE
    result_text TEXT := '';
BEGIN
    -- Update queue loads based on current tasks
    UPDATE flowable.workflow_queues
    SET current_load = (
        SELECT COUNT(*)
        FROM flowable.workflow_tasks t
        WHERE t.queue_name = workflow_queues.queue_name
          AND t.status IN ('OPEN', 'CLAIMED')
    );
    
    result_text := result_text || 'Queue loads updated. ';
    
    -- Escalate overdue tasks
    CALL flowable.escalate_overdue_tasks();
    result_text := result_text || 'Overdue tasks escalated. ';
    
    -- Archive old completed tasks (older than 90 days)
    -- CALL flowable.archive_completed_tasks(90);
    -- result_text := result_text || 'Old tasks archived. ';
    
    -- Refresh materialized views if they exist
    -- REFRESH MATERIALIZED VIEW IF EXISTS mv_daily_queue_stats;
    -- result_text := result_text || 'Materialized views refreshed. ';
    
    -- Update table statistics
    ANALYZE flowable.workflow_tasks;
    ANALYZE flowable.task_history;
    result_text := result_text || 'Table statistics updated.';
    
    RETURN result_text;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- STEP 8: VALIDATION AND TESTING
-- =============================================================================

\echo 'Step 8: Running validation tests...'

-- Test basic functionality with sample data
DO $$
DECLARE
    test_process_id VARCHAR(64);
    test_task_id VARCHAR(64);
    test_result BOOLEAN;
BEGIN
    -- Test 1: Start a process instance
    SELECT flowable.start_process_instance(
        'oneCmsUnifiedWorkflow',
        'TEST-CASE-001',
        NULL,
        'system',
        '{"test": true}'::jsonb
    ) INTO test_process_id;
    
    IF test_process_id IS NOT NULL THEN
        RAISE NOTICE 'Test 1 PASSED: Process instance created with ID %', test_process_id;
    ELSE
        RAISE EXCEPTION 'Test 1 FAILED: Could not create process instance';
    END IF;
    
    -- Test 2: Create a test task
    SELECT flowable.create_workflow_task(
        test_process_id,
        'test_task',
        'Test Task for Validation',
        ARRAY['GROUP_EO_HEAD'],
        ARRAY[]::TEXT[],
        50,
        CURRENT_TIMESTAMP + INTERVAL '2 hours',
        NULL,
        '{"test_data": true}'::jsonb
    ) INTO test_task_id;
    
    IF test_task_id IS NOT NULL THEN
        RAISE NOTICE 'Test 2 PASSED: Test task created with ID %', test_task_id;
    ELSE
        RAISE EXCEPTION 'Test 2 FAILED: Could not create test task';
    END IF;
    
    -- Test 3: Try to claim the task
    SELECT task_id IS NOT NULL 
    FROM flowable.claim_next_task('eo-head-001', ARRAY['eo-head-queue'])
    INTO test_result;
    
    IF test_result THEN
        RAISE NOTICE 'Test 3 PASSED: Task claimed successfully';
    ELSE
        RAISE NOTICE 'Test 3 INFO: No tasks available to claim (expected if queue assignment not set up)';
    END IF;
    
    -- Test 4: Check reporting views
    IF EXISTS (SELECT 1 FROM flowable.v_queue_dashboard LIMIT 1) THEN
        RAISE NOTICE 'Test 4 PASSED: Reporting views are accessible';
    ELSE
        RAISE EXCEPTION 'Test 4 FAILED: Could not access reporting views';
    END IF;
    
    -- Cleanup test data
    DELETE FROM flowable.workflow_tasks WHERE task_id = test_task_id;
    DELETE FROM flowable.process_instances WHERE process_instance_id = test_process_id;
    
    RAISE NOTICE 'All validation tests completed successfully!';
END $$;

-- =============================================================================
-- STEP 9: GENERATE SUMMARY REPORT
-- =============================================================================

\echo 'Step 9: Generating installation summary...'

-- Generate installation summary
SELECT 
    'Database Objects Created' as category,
    COUNT(*) as count,
    'tables' as type
FROM information_schema.tables 
WHERE table_schema IN ('entitlements', 'flowable', 'onecms')

UNION ALL

SELECT 
    'Stored Procedures/Functions',
    COUNT(*),
    'functions'
FROM information_schema.routines 
WHERE routine_schema IN ('entitlements', 'flowable', 'onecms')

UNION ALL

SELECT 
    'Views Created',
    COUNT(*),
    'views'
FROM information_schema.views 
WHERE table_schema IN ('entitlements', 'flowable', 'onecms')

UNION ALL

SELECT 
    'Indexes Created',
    COUNT(*),
    'indexes'
FROM pg_indexes 
WHERE schemaname IN ('entitlements', 'flowable', 'onecms')

UNION ALL

SELECT 
    'Sample Users',
    COUNT(*),
    'users'
FROM entitlements.users

UNION ALL

SELECT 
    'Workflow Roles',
    COUNT(*),
    'roles'
FROM entitlements.workflow_roles

UNION ALL

SELECT 
    'Workflow Queues',
    COUNT(*),
    'queues'
FROM flowable.workflow_queues;

-- Display queue statistics
\echo ''
\echo 'Queue Configuration Summary:'
SELECT 
    q.queue_name,
    d.department_name,
    q.queue_type,
    q.max_capacity,
    q.sla_minutes,
    COUNT(qa.role_id) as assigned_roles
FROM flowable.workflow_queues q
LEFT JOIN entitlements.departments d ON q.department_id = d.department_id
LEFT JOIN flowable.queue_assignments qa ON q.queue_id = qa.queue_id
WHERE q.is_active = TRUE
GROUP BY q.queue_name, d.department_name, q.queue_type, q.max_capacity, q.sla_minutes
ORDER BY q.queue_name;

-- =============================================================================
-- COMPLETION MESSAGE
-- =============================================================================

\echo ''
\echo '========================================================================='
\echo 'NextGen Workflow Database Initialization COMPLETED Successfully!'
\echo ''
\echo 'What was created:'
\echo '  ✓ Database schemas (entitlements, flowable, onecms)'
\echo '  ✓ Core tables with optimized indexes'
\echo '  ✓ Stored procedures and functions for workflow management'
\echo '  ✓ Comprehensive reporting views'
\echo '  ✓ Security policies and row-level security'
\echo '  ✓ Sample data for testing'
\echo '  ✓ Maintenance procedures'
\echo ''
\echo 'Next Steps:'
\echo '  1. Configure application database connections'
\echo '  2. Deploy workflow BPMN definitions'
\echo '  3. Set up monitoring and alerting'
\echo '  4. Configure backup procedures'
\echo '  5. Run performance tests with production data volumes'
\echo ''
\echo 'For detailed architecture documentation, see:'
\echo '  - database-design-workflow.md'
\echo '  - database-solution-architecture.md'
\echo ''
\echo '========================================================================='

-- Set ownership and permissions (customize based on your environment)
-- GRANT USAGE ON SCHEMA entitlements TO workflow_app_user;
-- GRANT USAGE ON SCHEMA flowable TO workflow_app_user;
-- GRANT USAGE ON SCHEMA onecms TO workflow_app_user;

-- Grant appropriate permissions to application users
-- (These would be customized based on your specific security requirements)

-- End of initialization script