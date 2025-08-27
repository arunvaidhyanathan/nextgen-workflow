-- =============================================================================
-- NextGen Workflow Management System - Database Views and Reporting
-- =============================================================================
-- Description: Reporting views and materialized views for workflow analytics
-- Version: 1.0
-- Created: 2025-08-27
-- =============================================================================

-- =============================================================================
-- OPERATIONAL VIEWS
-- =============================================================================

-- Active tasks by queue with workload metrics
CREATE OR REPLACE VIEW flowable.v_active_tasks_by_queue AS
SELECT 
    q.queue_name,
    q.queue_type,
    q.department_id,
    d.department_name,
    d.department_code,
    COUNT(CASE WHEN t.status = 'OPEN' THEN 1 END) as open_tasks,
    COUNT(CASE WHEN t.status = 'CLAIMED' THEN 1 END) as claimed_tasks,
    COUNT(CASE WHEN t.status = 'ESCALATED' THEN 1 END) as escalated_tasks,
    COUNT(CASE WHEN t.status = 'COMPLETED' AND t.completed_at > CURRENT_TIMESTAMP - INTERVAL '24 hours' THEN 1 END) as completed_today,
    q.max_capacity,
    q.current_load,
    ROUND((q.current_load::numeric / q.max_capacity::numeric) * 100, 2) as capacity_utilization_pct,
    q.sla_minutes,
    COUNT(CASE WHEN t.status IN ('OPEN', 'CLAIMED') AND t.due_date < CURRENT_TIMESTAMP THEN 1 END) as overdue_tasks,
    AVG(CASE WHEN t.status = 'COMPLETED' AND t.completed_at > CURRENT_TIMESTAMP - INTERVAL '7 days'
        THEN EXTRACT(EPOCH FROM (t.completed_at - t.created_at))/3600 
    END) as avg_completion_hours_last_week,
    MIN(CASE WHEN t.status = 'OPEN' THEN t.created_at END) as oldest_open_task,
    MAX(t.priority) as highest_priority_open
FROM flowable.workflow_queues q
LEFT JOIN entitlements.departments d ON q.department_id = d.department_id
LEFT JOIN flowable.workflow_tasks t ON q.queue_name = t.queue_name
WHERE q.is_active = TRUE
GROUP BY q.queue_name, q.queue_type, q.department_id, d.department_name, d.department_code, 
         q.max_capacity, q.current_load, q.sla_minutes
ORDER BY q.department_id, q.queue_name;

-- User workload and performance metrics
CREATE OR REPLACE VIEW flowable.v_user_workload AS
SELECT 
    u.user_id,
    u.username,
    u.first_name || ' ' || u.last_name as full_name,
    u.email,
    d.department_code,
    d.department_name,
    array_agg(DISTINCT wr.role_code ORDER BY wr.role_code) as assigned_roles,
    array_agg(DISTINCT wr.queue_name ORDER BY wr.queue_name) as assigned_queues,
    COUNT(CASE WHEN t.status = 'CLAIMED' THEN 1 END) as current_claimed_tasks,
    COUNT(CASE WHEN t.status = 'COMPLETED' 
          AND t.completed_at > CURRENT_TIMESTAMP - INTERVAL '7 days' THEN 1 END) as completed_last_week,
    COUNT(CASE WHEN t.status = 'COMPLETED' 
          AND t.completed_at > CURRENT_TIMESTAMP - INTERVAL '30 days' THEN 1 END) as completed_last_month,
    AVG(CASE WHEN t.status = 'COMPLETED' AND t.completed_at > CURRENT_TIMESTAMP - INTERVAL '30 days'
        THEN EXTRACT(EPOCH FROM (t.completed_at - t.claimed_at))/3600 
    END) as avg_task_duration_hours,
    COUNT(CASE WHEN t.status = 'COMPLETED' AND t.completed_at > t.due_date THEN 1 END) as overdue_completions,
    MAX(t.completed_at) as last_task_completion,
    CASE 
        WHEN COUNT(CASE WHEN t.status = 'CLAIMED' THEN 1 END) = 0 THEN 'AVAILABLE'
        WHEN COUNT(CASE WHEN t.status = 'CLAIMED' THEN 1 END) < 5 THEN 'LIGHT_LOAD'
        WHEN COUNT(CASE WHEN t.status = 'CLAIMED' THEN 1 END) < 10 THEN 'MODERATE_LOAD'
        ELSE 'HEAVY_LOAD'
    END as workload_status
FROM entitlements.users u
JOIN entitlements.user_workflow_roles uwr ON u.user_id = uwr.user_id
JOIN entitlements.workflow_roles wr ON uwr.role_id = wr.role_id
LEFT JOIN entitlements.departments d ON u.department_id = d.department_id
LEFT JOIN flowable.workflow_tasks t ON u.user_id = t.assignee_id
WHERE u.is_active = TRUE 
  AND (uwr.valid_until IS NULL OR uwr.valid_until > CURRENT_TIMESTAMP)
  AND wr.is_active = TRUE
GROUP BY u.user_id, u.username, u.first_name, u.last_name, u.email, 
         d.department_code, d.department_name
ORDER BY d.department_code, u.username;

-- Process performance metrics
CREATE OR REPLACE VIEW flowable.v_process_performance AS
SELECT 
    wd.process_key,
    wd.process_name,
    wd.business_app,
    wd.process_version,
    COUNT(pi.process_instance_id) as total_instances,
    COUNT(CASE WHEN pi.status = 'ACTIVE' THEN 1 END) as active_instances,
    COUNT(CASE WHEN pi.status = 'COMPLETED' THEN 1 END) as completed_instances,
    COUNT(CASE WHEN pi.status = 'SUSPENDED' THEN 1 END) as suspended_instances,
    COUNT(CASE WHEN pi.status = 'ERROR' THEN 1 END) as error_instances,
    COUNT(CASE WHEN pi.started_at > CURRENT_TIMESTAMP - INTERVAL '7 days' THEN 1 END) as started_last_week,
    COUNT(CASE WHEN pi.completed_at > CURRENT_TIMESTAMP - INTERVAL '7 days' THEN 1 END) as completed_last_week,
    AVG(CASE WHEN pi.status = 'COMPLETED' 
        THEN EXTRACT(EPOCH FROM (pi.completed_at - pi.started_at))/3600 
    END) as avg_completion_hours,
    PERCENTILE_CONT(0.5) WITHIN GROUP (
        ORDER BY CASE WHEN pi.status = 'COMPLETED' 
        THEN EXTRACT(EPOCH FROM (pi.completed_at - pi.started_at))/3600 END
    ) as median_completion_hours,
    PERCENTILE_CONT(0.95) WITHIN GROUP (
        ORDER BY CASE WHEN pi.status = 'COMPLETED' 
        THEN EXTRACT(EPOCH FROM (pi.completed_at - pi.started_at))/3600 END
    ) as p95_completion_hours,
    COUNT(CASE WHEN pi.status = 'COMPLETED' AND pi.completed_at <= pi.sla_due_date THEN 1 END)::numeric / 
        NULLIF(COUNT(CASE WHEN pi.status = 'COMPLETED' AND pi.sla_due_date IS NOT NULL THEN 1 END), 0) * 100 
        as sla_compliance_pct,
    wd.deployed_at,
    wd.deployed_by,
    wd.is_active
FROM flowable.workflow_definitions wd
LEFT JOIN flowable.process_instances pi ON wd.workflow_id = pi.workflow_id
GROUP BY wd.process_key, wd.process_name, wd.business_app, wd.process_version,
         wd.deployed_at, wd.deployed_by, wd.is_active
ORDER BY wd.business_app, wd.process_key, wd.process_version DESC;

-- Task aging and escalation analysis
CREATE OR REPLACE VIEW flowable.v_task_aging_analysis AS
SELECT 
    t.queue_name,
    t.status,
    t.priority,
    COUNT(*) as task_count,
    AVG(EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - t.created_at))/3600) as avg_age_hours,
    MIN(EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - t.created_at))/3600) as min_age_hours,
    MAX(EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - t.created_at))/3600) as max_age_hours,
    COUNT(CASE WHEN t.due_date < CURRENT_TIMESTAMP THEN 1 END) as overdue_count,
    COUNT(CASE WHEN t.escalation_level > 0 THEN 1 END) as escalated_count,
    PERCENTILE_CONT(0.5) WITHIN GROUP (
        ORDER BY EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - t.created_at))/3600
    ) as median_age_hours,
    PERCENTILE_CONT(0.95) WITHIN GROUP (
        ORDER BY EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - t.created_at))/3600
    ) as p95_age_hours
FROM flowable.workflow_tasks t
WHERE t.status IN ('OPEN', 'CLAIMED', 'ESCALATED')
GROUP BY t.queue_name, t.status, t.priority
ORDER BY t.queue_name, t.priority DESC, t.status;

-- Department workflow summary
CREATE OR REPLACE VIEW flowable.v_department_workflow_summary AS
SELECT 
    d.department_code,
    d.department_name,
    COUNT(DISTINCT wr.role_id) as total_roles,
    COUNT(DISTINCT q.queue_id) as total_queues,
    COUNT(DISTINCT uwr.user_id) as active_users,
    SUM(CASE WHEN t.status = 'OPEN' THEN 1 ELSE 0 END) as open_tasks,
    SUM(CASE WHEN t.status = 'CLAIMED' THEN 1 ELSE 0 END) as claimed_tasks,
    SUM(CASE WHEN t.status = 'ESCALATED' THEN 1 ELSE 0 END) as escalated_tasks,
    COUNT(CASE WHEN t.status = 'COMPLETED' 
          AND t.completed_at > CURRENT_TIMESTAMP - INTERVAL '7 days' THEN 1 END) as completed_last_week,
    AVG(q.sla_minutes) as avg_sla_minutes,
    SUM(q.max_capacity) as total_queue_capacity,
    SUM(q.current_load) as total_current_load,
    ROUND(
        (SUM(q.current_load)::numeric / NULLIF(SUM(q.max_capacity), 0)) * 100, 2
    ) as overall_utilization_pct
FROM entitlements.departments d
LEFT JOIN entitlements.workflow_roles wr ON d.department_id = wr.department_id AND wr.is_active = TRUE
LEFT JOIN flowable.workflow_queues q ON d.department_id = q.department_id AND q.is_active = TRUE
LEFT JOIN entitlements.user_workflow_roles uwr ON wr.role_id = uwr.role_id 
    AND (uwr.valid_until IS NULL OR uwr.valid_until > CURRENT_TIMESTAMP)
LEFT JOIN flowable.workflow_tasks t ON q.queue_name = t.queue_name
WHERE d.is_active = TRUE
GROUP BY d.department_code, d.department_name
ORDER BY d.department_code;

-- =============================================================================
-- MATERIALIZED VIEWS FOR PERFORMANCE
-- =============================================================================

-- Materialized view for dashboard metrics (refresh every 15 minutes)
CREATE MATERIALIZED VIEW flowable.mv_dashboard_metrics AS
SELECT 
    CURRENT_TIMESTAMP as last_refreshed,
    -- Overall system metrics
    (SELECT COUNT(*) FROM flowable.process_instances WHERE status = 'ACTIVE') as active_processes,
    (SELECT COUNT(*) FROM flowable.workflow_tasks WHERE status = 'OPEN') as open_tasks,
    (SELECT COUNT(*) FROM flowable.workflow_tasks WHERE status = 'CLAIMED') as claimed_tasks,
    (SELECT COUNT(*) FROM flowable.workflow_tasks WHERE status = 'ESCALATED') as escalated_tasks,
    (SELECT COUNT(*) FROM flowable.workflow_tasks 
     WHERE status IN ('OPEN', 'CLAIMED') AND due_date < CURRENT_TIMESTAMP) as overdue_tasks,
    
    -- Daily metrics
    (SELECT COUNT(*) FROM flowable.process_instances 
     WHERE started_at > CURRENT_DATE) as processes_started_today,
    (SELECT COUNT(*) FROM flowable.workflow_tasks 
     WHERE completed_at > CURRENT_DATE) as tasks_completed_today,
    
    -- Weekly metrics
    (SELECT COUNT(*) FROM flowable.process_instances 
     WHERE started_at > CURRENT_TIMESTAMP - INTERVAL '7 days') as processes_started_last_week,
    (SELECT COUNT(*) FROM flowable.workflow_tasks 
     WHERE completed_at > CURRENT_TIMESTAMP - INTERVAL '7 days') as tasks_completed_last_week,
    
    -- Performance metrics
    (SELECT AVG(EXTRACT(EPOCH FROM (completed_at - created_at))/3600)
     FROM flowable.workflow_tasks 
     WHERE completed_at > CURRENT_TIMESTAMP - INTERVAL '7 days') as avg_task_completion_hours_last_week,
    
    (SELECT AVG(EXTRACT(EPOCH FROM (completed_at - started_at))/3600)
     FROM flowable.process_instances 
     WHERE completed_at > CURRENT_TIMESTAMP - INTERVAL '7 days') as avg_process_completion_hours_last_week;

-- Index for materialized view
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_dashboard_metrics_refreshed 
    ON flowable.mv_dashboard_metrics (last_refreshed);

-- =============================================================================
-- REFRESH FUNCTIONS FOR MATERIALIZED VIEWS
-- =============================================================================

-- Function to refresh dashboard metrics
CREATE OR REPLACE FUNCTION flowable.refresh_dashboard_metrics()
RETURNS VOID AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY flowable.mv_dashboard_metrics;
    
    -- Log the refresh
    INSERT INTO flowable.system_log (log_type, message, created_at)
    VALUES ('MATERIALIZED_VIEW_REFRESH', 'Dashboard metrics refreshed', CURRENT_TIMESTAMP);
    
EXCEPTION
    WHEN OTHERS THEN
        -- Log any errors
        INSERT INTO flowable.system_log (log_type, message, error_details, created_at)
        VALUES ('MATERIALIZED_VIEW_ERROR', 'Failed to refresh dashboard metrics', SQLERRM, CURRENT_TIMESTAMP);
        RAISE;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- SYSTEM LOG TABLE FOR MAINTENANCE
-- =============================================================================

CREATE TABLE IF NOT EXISTS flowable.system_log (
    log_id BIGSERIAL PRIMARY KEY,
    log_type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    error_details TEXT,
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_system_log_type_created ON flowable.system_log(log_type, created_at DESC);

-- =============================================================================
-- COMMENTS
-- =============================================================================

COMMENT ON VIEW flowable.v_active_tasks_by_queue IS 'Real-time view of task distribution and workload across queues';
COMMENT ON VIEW flowable.v_user_workload IS 'User workload analysis with performance metrics and availability status';
COMMENT ON VIEW flowable.v_process_performance IS 'Process definition performance metrics including SLA compliance';
COMMENT ON VIEW flowable.v_task_aging_analysis IS 'Task aging analysis for identifying bottlenecks and escalation needs';
COMMENT ON VIEW flowable.v_department_workflow_summary IS 'Department-level workflow summary with capacity utilization';

COMMENT ON MATERIALIZED VIEW flowable.mv_dashboard_metrics IS 'Pre-computed dashboard metrics for fast loading (refreshed every 15 minutes)';

COMMENT ON FUNCTION flowable.refresh_dashboard_metrics() IS 'Refreshes the dashboard metrics materialized view';

COMMENT ON TABLE flowable.system_log IS 'System maintenance and operation log';

-- =============================================================================
-- SUCCESS MESSAGE
-- =============================================================================

DO $$
BEGIN
    RAISE NOTICE 'NextGen Workflow Database Views created successfully!';
    RAISE NOTICE 'Views created: v_active_tasks_by_queue, v_user_workload, v_process_performance';
    RAISE NOTICE 'Materialized views: mv_dashboard_metrics';
    RAISE NOTICE 'Functions: refresh_dashboard_metrics()';
END $$;