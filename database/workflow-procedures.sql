-- =============================================================================
-- NextGen Workflow Management System - Stored Procedures and Functions
-- =============================================================================
-- Description: Business logic procedures for workflow task management
-- Version: 1.0
-- Created: 2025-08-27
-- =============================================================================

-- =============================================================================
-- TASK MANAGEMENT PROCEDURES
-- =============================================================================

-- Claim the next available task from user's queues
CREATE OR REPLACE FUNCTION flowable.claim_next_task(
    p_user_id VARCHAR(50),
    p_queue_names TEXT[] DEFAULT NULL
) RETURNS TABLE (
    task_id VARCHAR(64),
    task_name VARCHAR(500),
    queue_name VARCHAR(100),
    priority INTEGER,
    created_at TIMESTAMP,
    due_date TIMESTAMP
) AS $$
DECLARE
    v_task_id VARCHAR(64);
    v_user_queues TEXT[];
BEGIN
    -- Get user's available queues if not specified
    IF p_queue_names IS NULL THEN
        SELECT array_agg(DISTINCT wr.queue_name)
        INTO v_user_queues
        FROM entitlements.user_workflow_roles uwr
        JOIN entitlements.workflow_roles wr ON uwr.role_id = wr.role_id
        WHERE uwr.user_id = p_user_id
          AND (uwr.valid_until IS NULL OR uwr.valid_until > CURRENT_TIMESTAMP)
          AND wr.is_active = TRUE
          AND wr.queue_name IS NOT NULL;
    ELSE
        v_user_queues := p_queue_names;
    END IF;

    -- Check if user has any available queues
    IF v_user_queues IS NULL OR array_length(v_user_queues, 1) = 0 THEN
        RAISE EXCEPTION 'User % has no available queues or roles', p_user_id;
    END IF;

    -- Select and lock the highest priority available task
    SELECT t.task_id INTO v_task_id
    FROM flowable.workflow_tasks t
    WHERE t.queue_name = ANY(v_user_queues)
      AND t.status = 'OPEN'
      AND (
          -- Check if user has role that matches candidate groups
          t.candidate_groups && (
              SELECT array_agg(wr.role_code)
              FROM entitlements.user_workflow_roles uwr
              JOIN entitlements.workflow_roles wr ON uwr.role_id = wr.role_id
              WHERE uwr.user_id = p_user_id
                AND (uwr.valid_until IS NULL OR uwr.valid_until > CURRENT_TIMESTAMP)
          ) 
          OR 
          -- Check if user is directly listed in candidate users
          t.candidate_users @> ARRAY[p_user_id]
          OR
          -- If no candidate restrictions, allow based on queue access
          (t.candidate_groups IS NULL AND t.candidate_users IS NULL)
      )
    ORDER BY t.priority DESC, t.created_at ASC
    LIMIT 1
    FOR UPDATE SKIP LOCKED;

    IF v_task_id IS NOT NULL THEN
        -- Update task status
        UPDATE flowable.workflow_tasks
        SET status = 'CLAIMED',
            assignee_id = p_user_id,
            claimed_at = CURRENT_TIMESTAMP,
            claimed_by = p_user_id
        WHERE task_id = v_task_id;

        -- Update queue load
        UPDATE flowable.workflow_queues
        SET current_load = current_load + 1
        WHERE queue_name = (SELECT queue_name FROM flowable.workflow_tasks WHERE task_id = v_task_id);

        -- Insert history record
        INSERT INTO flowable.task_history (
            task_id, process_instance_id, action_type, action_by,
            old_status, new_status, comments
        )
        SELECT t.task_id, t.process_instance_id, 'CLAIM', p_user_id,
               'OPEN', 'CLAIMED', 'Task claimed by user'
        FROM flowable.workflow_tasks t
        WHERE t.task_id = v_task_id;

        -- Return task details
        RETURN QUERY
        SELECT t.task_id, t.task_name, t.queue_name, t.priority, t.created_at, t.due_date
        FROM flowable.workflow_tasks t
        WHERE t.task_id = v_task_id;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Complete a workflow task
CREATE OR REPLACE FUNCTION flowable.complete_task(
    p_task_id VARCHAR(64),
    p_user_id VARCHAR(50),
    p_outcome VARCHAR(100) DEFAULT NULL,
    p_outcome_reason TEXT DEFAULT NULL,
    p_form_data JSONB DEFAULT NULL,
    p_variables JSONB DEFAULT NULL
) RETURNS BOOLEAN AS $$
DECLARE
    v_task_record RECORD;
    v_process_instance_id VARCHAR(64);
BEGIN
    -- Get task details and verify ownership
    SELECT task_id, process_instance_id, assignee_id, status, queue_name
    INTO v_task_record
    FROM flowable.workflow_tasks
    WHERE task_id = p_task_id;

    -- Check if task exists
    IF v_task_record.task_id IS NULL THEN
        RAISE EXCEPTION 'Task % not found', p_task_id;
    END IF;

    -- Check if task is assigned to the user
    IF v_task_record.assignee_id != p_user_id THEN
        RAISE EXCEPTION 'Task % is not assigned to user %', p_task_id, p_user_id;
    END IF;

    -- Check if task is in correct status
    IF v_task_record.status NOT IN ('CLAIMED', 'ESCALATED') THEN
        RAISE EXCEPTION 'Task % cannot be completed in status %', p_task_id, v_task_record.status;
    END IF;

    -- Update task as completed
    UPDATE flowable.workflow_tasks
    SET status = 'COMPLETED',
        completed_at = CURRENT_TIMESTAMP,
        completed_by = p_user_id,
        outcome = p_outcome,
        outcome_reason = p_outcome_reason,
        form_data = COALESCE(p_form_data, form_data)
    WHERE task_id = p_task_id;

    -- Update queue load
    UPDATE flowable.workflow_queues
    SET current_load = GREATEST(current_load - 1, 0)
    WHERE queue_name = v_task_record.queue_name;

    -- Store variables if provided
    IF p_variables IS NOT NULL THEN
        INSERT INTO flowable.workflow_variables (
            task_id, variable_name, variable_type, variable_json, updated_by
        )
        SELECT p_task_id, key, 'JSON', value, p_user_id
        FROM jsonb_each(p_variables);
    END IF;

    -- Insert completion history
    INSERT INTO flowable.task_history (
        task_id, process_instance_id, action_type, action_by,
        old_status, new_status, comments, action_data
    ) VALUES (
        p_task_id, v_task_record.process_instance_id, 'COMPLETE', p_user_id,
        v_task_record.status, 'COMPLETED', 
        COALESCE(p_outcome_reason, 'Task completed'),
        jsonb_build_object(
            'outcome', p_outcome,
            'form_data', p_form_data,
            'variables', p_variables
        )
    );

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Escalate overdue tasks
CREATE OR REPLACE PROCEDURE flowable.escalate_overdue_tasks()
AS $$
DECLARE
    v_task RECORD;
    v_escalation_config JSONB;
    v_escalate_to VARCHAR(50);
BEGIN
    FOR v_task IN 
        SELECT t.task_id, t.process_instance_id, t.queue_name, t.assignee_id,
               t.escalation_level, t.created_at, t.due_date,
               q.escalation_config
        FROM flowable.workflow_tasks t
        JOIN flowable.workflow_queues q ON t.queue_name = q.queue_name
        WHERE t.status IN ('OPEN', 'CLAIMED')
          AND t.due_date < CURRENT_TIMESTAMP
          AND t.escalation_level < 3
          AND q.is_active = TRUE
    LOOP
        -- Get escalation configuration
        v_escalation_config := v_task.escalation_config;
        
        -- Determine escalation target based on level and config
        v_escalate_to := CASE v_task.escalation_level
            WHEN 0 THEN COALESCE(v_escalation_config->>'level_1_escalate_to', 'MANAGER')
            WHEN 1 THEN COALESCE(v_escalation_config->>'level_2_escalate_to', 'DIRECTOR')
            WHEN 2 THEN COALESCE(v_escalation_config->>'level_3_escalate_to', 'ADMIN')
            ELSE 'ADMIN'
        END;

        -- Update task escalation
        UPDATE flowable.workflow_tasks
        SET escalation_level = escalation_level + 1,
            escalated_at = CURRENT_TIMESTAMP,
            status = 'ESCALATED',
            priority = LEAST(priority + 10, 100)  -- Increase priority but cap at 100
        WHERE task_id = v_task.task_id;

        -- Insert escalation history
        INSERT INTO flowable.task_history (
            task_id, process_instance_id, action_type, action_by,
            old_status, new_status, escalate_to, comments, action_data
        ) VALUES (
            v_task.task_id, v_task.process_instance_id, 'ESCALATE', 'SYSTEM',
            'CLAIMED', 'ESCALATED', v_escalate_to,
            format('Auto-escalated due to SLA breach. Level %s escalation.', v_task.escalation_level + 1),
            jsonb_build_object(
                'escalation_level', v_task.escalation_level + 1,
                'overdue_hours', EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - v_task.due_date))/3600,
                'escalate_to', v_escalate_to
            )
        );

        RAISE NOTICE 'Task % escalated to level % (escalate to: %)', 
            v_task.task_id, v_task.escalation_level + 1, v_escalate_to;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Delegate task to another user
CREATE OR REPLACE FUNCTION flowable.delegate_task(
    p_task_id VARCHAR(64),
    p_from_user_id VARCHAR(50),
    p_to_user_id VARCHAR(50),
    p_reason TEXT DEFAULT NULL
) RETURNS BOOLEAN AS $$
DECLARE
    v_task_record RECORD;
BEGIN
    -- Get and verify task
    SELECT task_id, process_instance_id, assignee_id, status, queue_name
    INTO v_task_record
    FROM flowable.workflow_tasks
    WHERE task_id = p_task_id;

    IF v_task_record.task_id IS NULL THEN
        RAISE EXCEPTION 'Task % not found', p_task_id;
    END IF;

    IF v_task_record.assignee_id != p_from_user_id THEN
        RAISE EXCEPTION 'Task % is not assigned to user %', p_task_id, p_from_user_id;
    END IF;

    IF v_task_record.status != 'CLAIMED' THEN
        RAISE EXCEPTION 'Task % cannot be delegated in status %', p_task_id, v_task_record.status;
    END IF;

    -- Verify target user has access to the queue
    IF NOT EXISTS (
        SELECT 1
        FROM entitlements.user_workflow_roles uwr
        JOIN entitlements.workflow_roles wr ON uwr.role_id = wr.role_id
        WHERE uwr.user_id = p_to_user_id
          AND wr.queue_name = v_task_record.queue_name
          AND (uwr.valid_until IS NULL OR uwr.valid_until > CURRENT_TIMESTAMP)
          AND wr.is_active = TRUE
    ) THEN
        RAISE EXCEPTION 'User % does not have access to queue %', p_to_user_id, v_task_record.queue_name;
    END IF;

    -- Update task assignment
    UPDATE flowable.workflow_tasks
    SET assignee_id = p_to_user_id,
        claimed_at = CURRENT_TIMESTAMP,
        claimed_by = p_to_user_id,
        status = 'DELEGATED'
    WHERE task_id = p_task_id;

    -- Insert delegation history
    INSERT INTO flowable.task_history (
        task_id, process_instance_id, action_type, action_by,
        old_assignee, new_assignee, delegate_to, comments
    ) VALUES (
        p_task_id, v_task_record.process_instance_id, 'DELEGATE', p_from_user_id,
        p_from_user_id, p_to_user_id, p_to_user_id,
        COALESCE(p_reason, 'Task delegated to another user')
    );

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Unclaim/release a task back to the queue
CREATE OR REPLACE FUNCTION flowable.unclaim_task(
    p_task_id VARCHAR(64),
    p_user_id VARCHAR(50),
    p_reason TEXT DEFAULT NULL
) RETURNS BOOLEAN AS $$
DECLARE
    v_task_record RECORD;
BEGIN
    -- Get and verify task
    SELECT task_id, process_instance_id, assignee_id, status, queue_name
    INTO v_task_record
    FROM flowable.workflow_tasks
    WHERE task_id = p_task_id;

    IF v_task_record.task_id IS NULL THEN
        RAISE EXCEPTION 'Task % not found', p_task_id;
    END IF;

    IF v_task_record.assignee_id != p_user_id THEN
        RAISE EXCEPTION 'Task % is not assigned to user %', p_task_id, p_user_id;
    END IF;

    IF v_task_record.status NOT IN ('CLAIMED', 'DELEGATED') THEN
        RAISE EXCEPTION 'Task % cannot be unclaimed in status %', p_task_id, v_task_record.status;
    END IF;

    -- Release task back to queue
    UPDATE flowable.workflow_tasks
    SET status = 'OPEN',
        assignee_id = NULL,
        claimed_at = NULL,
        claimed_by = NULL
    WHERE task_id = p_task_id;

    -- Update queue load
    UPDATE flowable.workflow_queues
    SET current_load = GREATEST(current_load - 1, 0)
    WHERE queue_name = v_task_record.queue_name;

    -- Insert unclaim history
    INSERT INTO flowable.task_history (
        task_id, process_instance_id, action_type, action_by,
        old_status, new_status, old_assignee, comments
    ) VALUES (
        p_task_id, v_task_record.process_instance_id, 'UNCLAIM', p_user_id,
        v_task_record.status, 'OPEN', p_user_id,
        COALESCE(p_reason, 'Task released back to queue')
    );

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- WORKFLOW PROCESS MANAGEMENT PROCEDURES
-- =============================================================================

-- Start a new workflow process instance
CREATE OR REPLACE FUNCTION flowable.start_process_instance(
    p_process_key VARCHAR(255),
    p_business_key VARCHAR(255),
    p_started_by VARCHAR(50),
    p_case_id BIGINT DEFAULT NULL,
    p_variables JSONB DEFAULT NULL,
    p_priority INTEGER DEFAULT 50
) RETURNS TABLE (
    process_instance_id VARCHAR(64),
    workflow_id BIGINT,
    status VARCHAR(50)
) AS $$
DECLARE
    v_workflow_id BIGINT;
    v_process_instance_id VARCHAR(64);
    v_sla_config JSONB;
    v_sla_due_date TIMESTAMP;
BEGIN
    -- Get workflow definition
    SELECT wd.workflow_id, wd.sla_config
    INTO v_workflow_id, v_sla_config
    FROM flowable.workflow_definitions wd
    WHERE wd.process_key = p_process_key
      AND wd.is_active = TRUE
    ORDER BY wd.process_version DESC
    LIMIT 1;

    IF v_workflow_id IS NULL THEN
        RAISE EXCEPTION 'Active workflow definition not found for process key: %', p_process_key;
    END IF;

    -- Generate process instance ID
    v_process_instance_id := 'proc_' || extract(epoch from now()) || '_' || floor(random() * 1000000);

    -- Calculate SLA due date
    IF v_sla_config->>'default_hours' IS NOT NULL THEN
        v_sla_due_date := CURRENT_TIMESTAMP + 
            (v_sla_config->>'default_hours')::integer * INTERVAL '1 hour';
    END IF;

    -- Insert process instance
    INSERT INTO flowable.process_instances (
        process_instance_id, workflow_id, case_id, business_key,
        started_by, status, variables, sla_due_date, priority
    ) VALUES (
        v_process_instance_id, v_workflow_id, p_case_id, p_business_key,
        p_started_by, 'ACTIVE', COALESCE(p_variables, '{}'::jsonb), 
        v_sla_due_date, p_priority
    );

    -- Store variables if provided
    IF p_variables IS NOT NULL THEN
        INSERT INTO flowable.workflow_variables (
            process_instance_id, variable_name, variable_type, variable_json, updated_by
        )
        SELECT v_process_instance_id, key, 'JSON', value, p_started_by
        FROM jsonb_each(p_variables);
    END IF;

    -- Create case workflow mapping if case_id provided
    IF p_case_id IS NOT NULL THEN
        INSERT INTO onecms.case_workflow_mapping (
            case_id, process_instance_id, workflow_type, initiated_by
        ) VALUES (
            p_case_id, v_process_instance_id, p_process_key, p_started_by
        );
    END IF;

    -- Return process instance details
    RETURN QUERY
    SELECT v_process_instance_id, v_workflow_id, 'ACTIVE'::VARCHAR(50);
END;
$$ LANGUAGE plpgsql;

-- Complete a process instance
CREATE OR REPLACE FUNCTION flowable.complete_process_instance(
    p_process_instance_id VARCHAR(64),
    p_completed_by VARCHAR(50),
    p_outcome VARCHAR(100) DEFAULT 'COMPLETED'
) RETURNS BOOLEAN AS $$
DECLARE
    v_process_record RECORD;
BEGIN
    -- Get process details
    SELECT process_instance_id, status, started_at
    INTO v_process_record
    FROM flowable.process_instances
    WHERE process_instance_id = p_process_instance_id;

    IF v_process_record.process_instance_id IS NULL THEN
        RAISE EXCEPTION 'Process instance % not found', p_process_instance_id;
    END IF;

    IF v_process_record.status != 'ACTIVE' THEN
        RAISE EXCEPTION 'Process instance % is not active (status: %)', 
            p_process_instance_id, v_process_record.status;
    END IF;

    -- Complete the process
    UPDATE flowable.process_instances
    SET status = 'COMPLETED',
        completed_at = CURRENT_TIMESTAMP
    WHERE process_instance_id = p_process_instance_id;

    -- Complete any remaining active tasks
    UPDATE flowable.workflow_tasks
    SET status = 'CANCELLED',
        completed_at = CURRENT_TIMESTAMP,
        completed_by = p_completed_by,
        outcome = 'PROCESS_COMPLETED'
    WHERE process_instance_id = p_process_instance_id
      AND status IN ('OPEN', 'CLAIMED', 'ESCALATED');

    -- Update case workflow mapping
    UPDATE onecms.case_workflow_mapping
    SET actual_completion = CURRENT_TIMESTAMP,
        current_phase = 'COMPLETED'
    WHERE process_instance_id = p_process_instance_id;

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- QUEUE MANAGEMENT PROCEDURES
-- =============================================================================

-- Rebalance queue loads and redistribute tasks
CREATE OR REPLACE PROCEDURE flowable.rebalance_queue_loads()
AS $$
DECLARE
    v_queue RECORD;
    v_overloaded_threshold NUMERIC := 0.9; -- 90% capacity
    v_underloaded_threshold NUMERIC := 0.5; -- 50% capacity
BEGIN
    FOR v_queue IN 
        SELECT q.queue_name, q.max_capacity, q.current_load,
               (q.current_load::NUMERIC / q.max_capacity::NUMERIC) as utilization
        FROM flowable.workflow_queues q
        WHERE q.is_active = TRUE
          AND q.max_capacity > 0
    LOOP
        -- Handle overloaded queues
        IF v_queue.utilization > v_overloaded_threshold THEN
            RAISE NOTICE 'Queue % is overloaded: %% utilization', 
                v_queue.queue_name, ROUND(v_queue.utilization * 100, 1);
            
            -- Logic for task redistribution could be added here
            -- For now, just log the condition
        END IF;

        -- Handle underloaded queues
        IF v_queue.utilization < v_underloaded_threshold AND v_queue.current_load > 0 THEN
            RAISE NOTICE 'Queue % is underloaded: %% utilization', 
                v_queue.queue_name, ROUND(v_queue.utilization * 100, 1);
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- MAINTENANCE AND CLEANUP PROCEDURES
-- =============================================================================

-- Archive completed tasks older than specified days
CREATE OR REPLACE PROCEDURE flowable.archive_completed_tasks(
    p_days_old INTEGER DEFAULT 90
)
AS $$
DECLARE
    v_archive_date TIMESTAMP;
    v_archived_count INTEGER := 0;
BEGIN
    v_archive_date := CURRENT_TIMESTAMP - (p_days_old || ' days')::INTERVAL;
    
    -- Create archive table if it doesn't exist
    CREATE TABLE IF NOT EXISTS flowable.workflow_tasks_archive (
        LIKE flowable.workflow_tasks INCLUDING ALL
    );

    -- Move old completed tasks to archive
    WITH moved_tasks AS (
        DELETE FROM flowable.workflow_tasks
        WHERE status = 'COMPLETED'
          AND completed_at < v_archive_date
        RETURNING *
    )
    INSERT INTO flowable.workflow_tasks_archive
    SELECT * FROM moved_tasks;

    GET DIAGNOSTICS v_archived_count = ROW_COUNT;

    -- Log the archival
    INSERT INTO flowable.system_log (log_type, message, metadata)
    VALUES ('TASK_ARCHIVAL', 
            format('Archived %s completed tasks older than %s days', v_archived_count, p_days_old),
            jsonb_build_object('archived_count', v_archived_count, 'days_old', p_days_old, 'archive_date', v_archive_date));

    RAISE NOTICE 'Archived % completed tasks older than % days', v_archived_count, p_days_old;
END;
$$ LANGUAGE plpgsql;

-- Update queue statistics and current loads
CREATE OR REPLACE PROCEDURE flowable.update_queue_statistics()
AS $$
BEGIN
    -- Update current load for all queues based on actual task counts
    UPDATE flowable.workflow_queues q
    SET current_load = (
        SELECT COUNT(*)
        FROM flowable.workflow_tasks t
        WHERE t.queue_name = q.queue_name
          AND t.status IN ('CLAIMED', 'ESCALATED')
    ),
    updated_at = CURRENT_TIMESTAMP
    WHERE q.is_active = TRUE;

    -- Log the update
    INSERT INTO flowable.system_log (log_type, message)
    VALUES ('QUEUE_STATS_UPDATE', 'Queue statistics updated');
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- UTILITY FUNCTIONS
-- =============================================================================

-- Get user's available tasks with filtering
CREATE OR REPLACE FUNCTION flowable.get_user_available_tasks(
    p_user_id VARCHAR(50),
    p_queue_names TEXT[] DEFAULT NULL,
    p_priority_min INTEGER DEFAULT 0,
    p_limit INTEGER DEFAULT 50
) RETURNS TABLE (
    task_id VARCHAR(64),
    task_name VARCHAR(500),
    queue_name VARCHAR(100),
    priority INTEGER,
    status VARCHAR(50),
    created_at TIMESTAMP,
    due_date TIMESTAMP,
    escalation_level INTEGER
) AS $$
DECLARE
    v_user_queues TEXT[];
BEGIN
    -- Get user's queues if not specified
    IF p_queue_names IS NULL THEN
        SELECT array_agg(DISTINCT wr.queue_name)
        INTO v_user_queues
        FROM entitlements.user_workflow_roles uwr
        JOIN entitlements.workflow_roles wr ON uwr.role_id = wr.role_id
        WHERE uwr.user_id = p_user_id
          AND (uwr.valid_until IS NULL OR uwr.valid_until > CURRENT_TIMESTAMP)
          AND wr.is_active = TRUE
          AND wr.queue_name IS NOT NULL;
    ELSE
        v_user_queues := p_queue_names;
    END IF;

    RETURN QUERY
    SELECT t.task_id, t.task_name, t.queue_name, t.priority, t.status,
           t.created_at, t.due_date, t.escalation_level
    FROM flowable.workflow_tasks t
    WHERE t.queue_name = ANY(v_user_queues)
      AND t.status = 'OPEN'
      AND t.priority >= p_priority_min
      AND (
          t.candidate_groups && (
              SELECT array_agg(wr.role_code)
              FROM entitlements.user_workflow_roles uwr
              JOIN entitlements.workflow_roles wr ON uwr.role_id = wr.role_id
              WHERE uwr.user_id = p_user_id
                AND (uwr.valid_until IS NULL OR uwr.valid_until > CURRENT_TIMESTAMP)
          ) 
          OR t.candidate_users @> ARRAY[p_user_id]
          OR (t.candidate_groups IS NULL AND t.candidate_users IS NULL)
      )
    ORDER BY t.priority DESC, t.created_at ASC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- COMMENTS AND DOCUMENTATION
-- =============================================================================

COMMENT ON FUNCTION flowable.claim_next_task(VARCHAR, TEXT[]) IS 'Claims the next available high-priority task for a user from their accessible queues';
COMMENT ON FUNCTION flowable.complete_task(VARCHAR, VARCHAR, VARCHAR, TEXT, JSONB, JSONB) IS 'Completes a workflow task with outcome and form data';
COMMENT ON PROCEDURE flowable.escalate_overdue_tasks() IS 'Automatically escalates tasks that have exceeded their SLA due dates';
COMMENT ON FUNCTION flowable.delegate_task(VARCHAR, VARCHAR, VARCHAR, TEXT) IS 'Delegates a task from one user to another with validation';
COMMENT ON FUNCTION flowable.unclaim_task(VARCHAR, VARCHAR, TEXT) IS 'Releases a claimed task back to the queue for others to claim';

COMMENT ON FUNCTION flowable.start_process_instance(VARCHAR, VARCHAR, VARCHAR, BIGINT, JSONB, INTEGER) IS 'Starts a new workflow process instance with variables and SLA configuration';
COMMENT ON FUNCTION flowable.complete_process_instance(VARCHAR, VARCHAR, VARCHAR) IS 'Completes a process instance and cancels remaining tasks';

COMMENT ON PROCEDURE flowable.rebalance_queue_loads() IS 'Analyzes and rebalances workload across queues';
COMMENT ON PROCEDURE flowable.archive_completed_tasks(INTEGER) IS 'Archives completed tasks older than specified days to reduce table size';
COMMENT ON PROCEDURE flowable.update_queue_statistics() IS 'Updates queue statistics and current load counts';

COMMENT ON FUNCTION flowable.get_user_available_tasks(VARCHAR, TEXT[], INTEGER, INTEGER) IS 'Returns available tasks for a user with filtering options';

-- =============================================================================
-- SUCCESS MESSAGE
-- =============================================================================

DO $$
BEGIN
    RAISE NOTICE 'NextGen Workflow Stored Procedures created successfully!';
    RAISE NOTICE 'Task Management: claim_next_task, complete_task, delegate_task, unclaim_task';
    RAISE NOTICE 'Process Management: start_process_instance, complete_process_instance';
    RAISE NOTICE 'Queue Management: rebalance_queue_loads, update_queue_statistics';
    RAISE NOTICE 'Maintenance: escalate_overdue_tasks, archive_completed_tasks';
    RAISE NOTICE 'Utilities: get_user_available_tasks';
END $$;