-- Enhanced Workflow Metadata Registration Script
-- This script registers the OneCMS Enhanced Workflow with comprehensive mappings
-- Execute this script after deploying the enhanced BPMN

-- =======================================
-- 1. REGISTER ENHANCED ONECMS WORKFLOW METADATA
-- =======================================

INSERT INTO workflow_metadata (
    process_definition_key,
    process_definition_name,
    business_app_name,
    version,
    description,
    candidate_group_mappings,
    deployment_status,
    created_at,
    updated_at,
    metadata
) VALUES (
    'oneCmsEnhancedWorkflow',
    'OneCMS Enhanced Case Workflow',
    'OneCMS',
    '2.0',
    'Enhanced comprehensive case management workflow with parallel processing, multiple end states, and full Figma alignment',
    -- Complete candidate group to queue mappings
    '{
        "GROUP_EO_HEAD": "eo-head-queue",
        "GROUP_EO_OFFICER": "eo-officer-queue",
        "GROUP_EO_INTAKE_ANALYST": "eo-intake-analyst-queue",
        "GROUP_CSIS_INTAKE_MANAGER": "csis-intake-manager-queue",
        "GROUP_CSIS_INTAKE_ANALYST": "csis-intake-analyst-queue",
        "GROUP_ER_INTAKE_ANALYST": "er-intake-analyst-queue",
        "GROUP_LEGAL_INTAKE_ANALYST": "legal-intake-analyst-queue",
        "GROUP_INVESTIGATION_MANAGER": "investigation-manager-queue",
        "GROUP_INVESTIGATOR": "investigator-queue"
    }'::jsonb,
    'DEPLOYED',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    '{
        "entry_points": ["EO_INITIATED", "CSIS_MANAGER_INITIATED", "CSIS_ANALYST_INITIATED", "ER_INITIATED", "LEGAL_INITIATED"],
        "departments": ["EO", "CSIS", "ER", "LEGAL", "INVESTIGATION"],
        "bpmn_file": "OneCMS_Nextgen_WF_Enhanced.bpmn20.xml",
        "parallel_processing": true,
        "multi_department_routing": true,
        "case_states": ["INTAKE", "DRAFT", "PARKED", "ACTIVE_INVESTIGATION", "COMPLETED", "CANCELLED"],
        "end_event_types": [
            "CASE_COMPLETED",
            "CASE_CANCELLED", 
            "CASE_PARKED",
            "RETAINED_FOR_INTELLIGENCE",
            "SENT_TO_HR",
            "NOT_VALID_CASE"
        ],
        "user_actions": {
            "common": ["SAVE", "SAVE_CLOSE", "CANCEL"],
            "intake_actions": ["CREATE", "NOT_VALID", "OTHER_CORRESPONDENCE"],
            "triage_actions": ["ASSIGN", "SEND_BACK", "REROUTE", "ESCALATE", "FAST_TRACK", "RETAIN_INTEL"],
            "investigation_actions": ["ACCEPT", "REJECT", "CONTINUE", "FINALIZE"]
        },
        "form_keys": {
            "task_eo_intake_initial": "eo-initial-intake-form",
            "task_eo_create_new_case": "eo-create-case-form", 
            "task_eo_enter_information": "eo-case-details-entry-form",
            "task_eo_assign_head": "eo-head-assignment-form",
            "task_eo_head_review": "eo-head-review-form",
            "task_eo_officer_triage": "eo-officer-triage-form",
            "task_csis_manager_create": "csis-manager-create-form",
            "task_csis_manager_enter_info": "csis-manager-details-form",
            "task_csis_analyst_create": "csis-analyst-create-form",
            "task_csis_analyst_enter_info": "csis-analyst-details-form",
            "task_csis_analyst_vetting": "csis-analyst-vetting-form",
            "task_csis_manager_approval": "csis-manager-approval-form",
            "task_csis_dept_initial_triage": "csis-dept-triage-form",
            "task_csis_dept_detailed_analysis": "csis-dept-analysis-form",
            "task_csis_retain_intel": "csis-retain-intel-form",
            "task_csis_manager_final_review": "csis-manager-final-form",
            "task_er_create_new_case": "er-create-case-form",
            "task_er_enter_information": "er-case-details-form", 
            "task_er_intake_review": "er-intake-review-form",
            "task_legal_create_new_case": "legal-create-case-form",
            "task_legal_enter_information": "legal-case-details-form",
            "task_legal_intake_review": "legal-intake-review-form",
            "task_inv_manager_review": "inv-manager-review-form",
            "task_investigator_assignment_review": "investigator-assignment-form", 
            "task_investigator_create_plan": "investigator-plan-creation-form",
            "task_conduct_investigation": "investigation-conduct-form",
            "task_escalation_review": "escalation-review-form",
            "task_finalize_investigation": "investigation-finalize-form"
        },
        "process_variables": {
            "caseState": "string",
            "casePriority": "string", 
            "departmentsInvolved": "string",
            "escalationLevel": "integer",
            "eoIntakeAction": "string",
            "eoUserAction": "string",
            "eoHeadAction": "string", 
            "eoOfficerAction": "string",
            "csisMgrAction": "string",
            "csisAnalystAction": "string",
            "csisVettingAction": "string",
            "csisFinalAction": "string",
            "csisDeptAction": "string",
            "csisAnalysisAction": "string",
            "csisMgrFinalAction": "string",
            "erUserAction": "string",
            "erIntakeAction": "string",
            "erFlowType": "string",
            "legalUserAction": "string", 
            "legalIntakeAction": "string",
            "legalFlowType": "string",
            "invMgrAction": "string",
            "investigatorAction": "string"
        },
        "escalation_timers": {
            "investigation_escalation": "P30D",
            "intake_sla": "P1D",
            "triage_sla": "P3D",
            "assignment_sla": "P7D"
        }
    }'::jsonb
)
ON CONFLICT (process_definition_key, business_app_name) 
DO UPDATE SET
    process_definition_name = EXCLUDED.process_definition_name,
    version = EXCLUDED.version,
    description = EXCLUDED.description,
    candidate_group_mappings = EXCLUDED.candidate_group_mappings,
    deployment_status = EXCLUDED.deployment_status,
    updated_at = CURRENT_TIMESTAMP,
    metadata = EXCLUDED.metadata;

-- =======================================
-- 2. REGISTER ENHANCED QUEUE TASK MAPPINGS
-- =======================================

-- Clear existing mappings for enhanced workflow
DELETE FROM queue_tasks 
WHERE process_definition_key = 'oneCmsEnhancedWorkflow'
  AND business_app_name = 'OneCMS';

-- Insert comprehensive task-to-queue mappings for enhanced workflow
INSERT INTO queue_tasks (
    process_definition_key,
    business_app_name,
    task_definition_key,
    queue_name,
    candidate_group,
    task_type,
    priority_weight,
    sla_minutes,
    escalation_config,
    created_at,
    updated_at,
    metadata
) VALUES

-- EO Intake Analyst Tasks
('oneCmsEnhancedWorkflow', 'OneCMS', 'task_eo_intake_initial', 'eo-intake-analyst-queue', 'GROUP_EO_INTAKE_ANALYST', 'INTAKE_INITIAL', 90, 240, 
 '{"escalation_levels": [{"level": 1, "minutes": 240, "notify": ["supervisor"]}, {"level": 2, "minutes": 480, "notify": ["manager"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "eo-initial-intake-form", "user_actions": ["CREATE", "NOT_VALID", "OTHER_CORRESPONDENCE"]}'::jsonb),

('oneCmsEnhancedWorkflow', 'OneCMS', 'task_eo_create_new_case', 'eo-intake-analyst-queue', 'GROUP_EO_INTAKE_ANALYST', 'CREATE_CASE', 85, 180,
 '{"escalation_levels": [{"level": 1, "minutes": 180, "notify": ["supervisor"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "eo-create-case-form", "user_actions": ["CREATE", "SAVE", "CANCEL"]}'::jsonb),

('oneCmsEnhancedWorkflow', 'OneCMS', 'task_eo_enter_information', 'eo-intake-analyst-queue', 'GROUP_EO_INTAKE_ANALYST', 'DATA_ENTRY', 80, 360,
 '{"escalation_levels": [{"level": 1, "minutes": 360, "notify": ["supervisor"]}, {"level": 2, "minutes": 720, "notify": ["manager"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "eo-case-details-entry-form", "user_actions": ["CREATE", "SAVE", "CANCEL"]}'::jsonb),

-- EO Head Tasks
('oneCmsEnhancedWorkflow', 'OneCMS', 'task_eo_assign_head', 'eo-head-queue', 'GROUP_EO_HEAD', 'ASSIGNMENT', 95, 480,
 '{"escalation_levels": [{"level": 1, "minutes": 480, "notify": ["director"]}, {"level": 2, "minutes": 960, "notify": ["executive"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "eo-head-assignment-form", "user_actions": ["ASSIGN", "REJECT", "CANCEL"]}'::jsonb),

('oneCmsEnhancedWorkflow', 'OneCMS', 'task_eo_head_review', 'eo-head-queue', 'GROUP_EO_HEAD', 'STRATEGIC_REVIEW', 100, 720,
 '{"escalation_levels": [{"level": 1, "minutes": 720, "notify": ["director"]}, {"level": 2, "minutes": 1440, "notify": ["executive"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "eo-head-review-form", "user_actions": ["APPROVE", "REJECT", "CANCEL"]}'::jsonb),

-- EO Officer Tasks
('oneCmsEnhancedWorkflow', 'OneCMS', 'task_eo_officer_triage', 'eo-officer-queue', 'GROUP_EO_OFFICER', 'TRIAGE_ROUTING', 90, 480,
 '{"escalation_levels": [{"level": 1, "minutes": 480, "notify": ["head"]}, {"level": 2, "minutes": 960, "notify": ["director"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "eo-officer-triage-form", "user_actions": ["SEND_TO_IU", "SEND_BACK", "SAVE_CLOSE", "CANCEL"]}'::jsonb),

-- CSIS Manager Tasks
('oneCmsEnhancedWorkflow', 'OneCMS', 'task_csis_manager_create', 'csis-intake-manager-queue', 'GROUP_CSIS_INTAKE_MANAGER', 'CREATE_CASE', 85, 300,
 '{"escalation_levels": [{"level": 1, "minutes": 300, "notify": ["director"]}, {"level": 2, "minutes": 600, "notify": ["security_head"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "csis-manager-create-form", "user_actions": ["CREATE", "SAVE", "CANCEL"]}'::jsonb),

('oneCmsEnhancedWorkflow', 'OneCMS', 'task_csis_manager_enter_info', 'csis-intake-manager-queue', 'GROUP_CSIS_INTAKE_MANAGER', 'DATA_ENTRY', 80, 240,
 '{"escalation_levels": [{"level": 1, "minutes": 240, "notify": ["director"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "csis-manager-details-form", "user_actions": ["ASSIGN_ANALYST", "FAST_TRACK", "RETAIN_INTEL", "SAVE", "CANCEL"]}'::jsonb),

('oneCmsEnhancedWorkflow', 'OneCMS', 'task_csis_manager_approval', 'csis-intake-manager-queue', 'GROUP_CSIS_INTAKE_MANAGER', 'APPROVAL', 95, 480,
 '{"escalation_levels": [{"level": 1, "minutes": 480, "notify": ["director"]}, {"level": 2, "minutes": 960, "notify": ["security_head"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "csis-manager-approval-form", "user_actions": ["APPROVE", "REJECT", "SAVE", "CANCEL"]}'::jsonb),

('oneCmsEnhancedWorkflow', 'OneCMS', 'task_csis_dept_initial_triage', 'csis-intake-manager-queue', 'GROUP_CSIS_INTAKE_MANAGER', 'TRIAGE', 90, 360,
 '{"escalation_levels": [{"level": 1, "minutes": 360, "notify": ["director"]}, {"level": 2, "minutes": 720, "notify": ["security_head"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "csis-dept-triage-form", "user_actions": ["ASSIGN", "SEND_BACK", "REROUTE", "SAVE", "TRIAGE_ESCALATE"]}'::jsonb),

('oneCmsEnhancedWorkflow', 'OneCMS', 'task_csis_manager_final_review', 'csis-intake-manager-queue', 'GROUP_CSIS_INTAKE_MANAGER', 'FINAL_REVIEW', 95, 480,
 '{"escalation_levels": [{"level": 1, "minutes": 480, "notify": ["director"]}, {"level": 2, "minutes": 960, "notify": ["security_head"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "csis-manager-final-form", "user_actions": ["APPROVE", "REJECT"]}'::jsonb),

-- CSIS Analyst Tasks
('oneCmsEnhancedWorkflow', 'OneCMS', 'task_csis_analyst_create', 'csis-intake-analyst-queue', 'GROUP_CSIS_INTAKE_ANALYST', 'CREATE_CASE', 75, 240,
 '{"escalation_levels": [{"level": 1, "minutes": 240, "notify": ["manager"]}, {"level": 2, "minutes": 480, "notify": ["director"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "csis-analyst-create-form", "user_actions": ["CREATE", "SAVE", "CANCEL"]}'::jsonb),

('oneCmsEnhancedWorkflow', 'OneCMS', 'task_csis_analyst_enter_info', 'csis-intake-analyst-queue', 'GROUP_CSIS_INTAKE_ANALYST', 'DATA_ENTRY', 70, 180,
 '{"escalation_levels": [{"level": 1, "minutes": 180, "notify": ["manager"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "csis-analyst-details-form", "user_actions": ["ASSIGN_MANAGER", "RETAIN_INTEL", "SAVE", "CANCEL"]}'::jsonb),

('oneCmsEnhancedWorkflow', 'OneCMS', 'task_csis_analyst_vetting', 'csis-intake-analyst-queue', 'GROUP_CSIS_INTAKE_ANALYST', 'VETTING', 80, 720,
 '{"escalation_levels": [{"level": 1, "minutes": 720, "notify": ["manager"]}, {"level": 2, "minutes": 1440, "notify": ["director"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "csis-analyst-vetting-form", "user_actions": ["RECOMMEND", "REJECT", "SAVE", "CANCEL"]}'::jsonb),

('oneCmsEnhancedWorkflow', 'OneCMS', 'task_csis_dept_detailed_analysis', 'csis-intake-analyst-queue', 'GROUP_CSIS_INTAKE_ANALYST', 'ANALYSIS', 85, 960,
 '{"escalation_levels": [{"level": 1, "minutes": 960, "notify": ["manager"]}, {"level": 2, "minutes": 1920, "notify": ["director"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "csis-dept-analysis-form", "user_actions": ["ESCALATE", "RETAIN_INTEL", "SAVE", "CANCEL"]}'::jsonb),

('oneCmsEnhancedWorkflow', 'OneCMS', 'task_csis_retain_intel', 'csis-intake-analyst-queue', 'GROUP_CSIS_INTAKE_ANALYST', 'INTELLIGENCE', 60, 480,
 '{"escalation_levels": [{"level": 1, "minutes": 480, "notify": ["manager"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "csis-retain-intel-form", "user_actions": ["RETAIN"]}'::jsonb),

-- ER Intake Analyst Tasks
('oneCmsEnhancedWorkflow', 'OneCMS', 'task_er_create_new_case', 'er-intake-analyst-queue', 'GROUP_ER_INTAKE_ANALYST', 'CREATE_CASE', 80, 240,
 '{"escalation_levels": [{"level": 1, "minutes": 240, "notify": ["hr_manager"]}, {"level": 2, "minutes": 480, "notify": ["hr_director"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "er-create-case-form", "user_actions": ["CREATE", "SAVE", "CANCEL"]}'::jsonb),

('oneCmsEnhancedWorkflow', 'OneCMS', 'task_er_enter_information', 'er-intake-analyst-queue', 'GROUP_ER_INTAKE_ANALYST', 'DATA_ENTRY', 75, 180,
 '{"escalation_levels": [{"level": 1, "minutes": 180, "notify": ["hr_manager"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "er-case-details-form", "user_actions": ["CREATE", "SAVE", "CANCEL"]}'::jsonb),

('oneCmsEnhancedWorkflow', 'OneCMS', 'task_er_intake_review', 'er-intake-analyst-queue', 'GROUP_ER_INTAKE_ANALYST', 'INTAKE_REVIEW', 85, 360,
 '{"escalation_levels": [{"level": 1, "minutes": 360, "notify": ["hr_manager"]}, {"level": 2, "minutes": 720, "notify": ["hr_director"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "er-intake-review-form", "user_actions": ["SEND_BACK", "ASSIGN_MANAGER", "ASSIGN_HR", "SAVE"]}'::jsonb),

-- Legal Intake Analyst Tasks
('oneCmsEnhancedWorkflow', 'OneCMS', 'task_legal_create_new_case', 'legal-intake-analyst-queue', 'GROUP_LEGAL_INTAKE_ANALYST', 'CREATE_CASE', 85, 360,
 '{"escalation_levels": [{"level": 1, "minutes": 360, "notify": ["legal_manager"]}, {"level": 2, "minutes": 720, "notify": ["general_counsel"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "legal-create-case-form", "user_actions": ["CREATE", "SAVE", "CANCEL"]}'::jsonb),

('oneCmsEnhancedWorkflow', 'OneCMS', 'task_legal_enter_information', 'legal-intake-analyst-queue', 'GROUP_LEGAL_INTAKE_ANALYST', 'DATA_ENTRY', 80, 240,
 '{"escalation_levels": [{"level": 1, "minutes": 240, "notify": ["legal_manager"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "legal-case-details-form", "user_actions": ["CREATE", "SAVE", "CANCEL"]}'::jsonb),

('oneCmsEnhancedWorkflow', 'OneCMS', 'task_legal_intake_review', 'legal-intake-analyst-queue', 'GROUP_LEGAL_INTAKE_ANALYST', 'LEGAL_REVIEW', 90, 720,
 '{"escalation_levels": [{"level": 1, "minutes": 720, "notify": ["legal_manager"]}, {"level": 2, "minutes": 1440, "notify": ["general_counsel"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "legal-intake-review-form", "user_actions": ["SEND_BACK", "ASSIGN_MANAGER", "SAVE"]}'::jsonb),

-- Investigation Manager Tasks
('oneCmsEnhancedWorkflow', 'OneCMS', 'task_inv_manager_review', 'investigation-manager-queue', 'GROUP_INVESTIGATION_MANAGER', 'REVIEW_ASSIGNMENT', 95, 480,
 '{"escalation_levels": [{"level": 1, "minutes": 480, "notify": ["director"]}, {"level": 2, "minutes": 960, "notify": ["chief_investigator"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "inv-manager-review-form", "user_actions": ["SEND_BACK", "ASSIGN", "SAVE"]}'::jsonb),

-- Investigator Tasks  
('oneCmsEnhancedWorkflow', 'OneCMS', 'task_investigator_assignment_review', 'investigator-queue', 'GROUP_INVESTIGATOR', 'ASSIGNMENT_REVIEW', 70, 240,
 '{"escalation_levels": [{"level": 1, "minutes": 240, "notify": ["manager"]}, {"level": 2, "minutes": 480, "notify": ["director"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "investigator-assignment-form", "user_actions": ["ACCEPT", "REJECT", "SAVE"]}'::jsonb),

('oneCmsEnhancedWorkflow', 'OneCMS', 'task_investigator_create_plan', 'investigator-queue', 'GROUP_INVESTIGATOR', 'PLAN_CREATION', 80, 720,
 '{"escalation_levels": [{"level": 1, "minutes": 720, "notify": ["manager"]}, {"level": 2, "minutes": 1440, "notify": ["director"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "investigator-plan-creation-form", "user_actions": ["CREATE_PLAN", "SAVE"]}'::jsonb),

-- Investigation Subprocess Tasks
('oneCmsEnhancedWorkflow', 'OneCMS', 'task_conduct_investigation', 'investigator-queue', 'GROUP_INVESTIGATOR', 'INVESTIGATION_CONDUCT', 90, 43200,
 '{"escalation_levels": [{"level": 1, "minutes": 43200, "notify": ["manager"]}, {"level": 2, "minutes": 86400, "notify": ["director"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "investigation-conduct-form", "user_actions": ["CONTINUE", "FINALIZE"], "timer_escalation": "P30D"}'::jsonb),

('oneCmsEnhancedWorkflow', 'OneCMS', 'task_escalation_review', 'investigation-manager-queue', 'GROUP_INVESTIGATION_MANAGER', 'ESCALATION_REVIEW', 100, 240,
 '{"escalation_levels": [{"level": 1, "minutes": 240, "notify": ["director"]}, {"level": 2, "minutes": 480, "notify": ["chief_investigator"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "escalation-review-form", "user_actions": ["CONTINUE", "REASSIGN", "ESCALATE"]}'::jsonb),

('oneCmsEnhancedWorkflow', 'OneCMS', 'task_finalize_investigation', 'investigator-queue', 'GROUP_INVESTIGATOR', 'INVESTIGATION_FINALIZE', 95, 480,
 '{"escalation_levels": [{"level": 1, "minutes": 480, "notify": ["manager"]}, {"level": 2, "minutes": 960, "notify": ["director"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "investigation-finalize-form", "user_actions": ["FINALIZE", "SUBMIT"]}'::jsonb);

-- =======================================
-- 3. ADD CASE STATE MANAGEMENT TABLES
-- =======================================

-- Add case state tracking table if it doesn't exist
CREATE TABLE IF NOT EXISTS case_state_transitions (
    transition_id SERIAL PRIMARY KEY,
    case_id BIGINT NOT NULL,
    process_instance_id VARCHAR(64) NOT NULL,
    from_state VARCHAR(50),
    to_state VARCHAR(50) NOT NULL,
    transition_reason VARCHAR(255),
    transition_by VARCHAR(100) NOT NULL,
    transition_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

-- Add process variables tracking table if it doesn't exist  
CREATE TABLE IF NOT EXISTS process_variable_audit (
    audit_id SERIAL PRIMARY KEY,
    process_instance_id VARCHAR(64) NOT NULL,
    variable_name VARCHAR(100) NOT NULL,
    variable_value TEXT,
    variable_type VARCHAR(50),
    updated_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    task_id VARCHAR(64),
    metadata JSONB
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_case_state_transitions_case_id ON case_state_transitions(case_id);
CREATE INDEX IF NOT EXISTS idx_case_state_transitions_process_instance ON case_state_transitions(process_instance_id);
CREATE INDEX IF NOT EXISTS idx_process_variable_audit_process_instance ON process_variable_audit(process_instance_id);
CREATE INDEX IF NOT EXISTS idx_process_variable_audit_variable_name ON process_variable_audit(variable_name);

-- =======================================
-- 4. VERIFY THE ENHANCED REGISTRATION
-- =======================================

-- Verify enhanced workflow metadata registration
SELECT 
    'Enhanced Workflow Metadata:' as check_type,
    process_definition_key,
    process_definition_name,
    business_app_name,
    version,
    deployment_status,
    jsonb_array_length(candidate_group_mappings::jsonb) as mapping_count,
    (metadata->>'parallel_processing')::boolean as parallel_processing,
    (metadata->>'multi_department_routing')::boolean as multi_department_routing
FROM workflow_metadata 
WHERE process_definition_key = 'oneCmsEnhancedWorkflow';

-- Verify enhanced candidate group mappings
SELECT 
    'Enhanced Candidate Mappings:' as check_type,
    key as candidate_group,
    value as queue_name
FROM workflow_metadata wm
CROSS JOIN LATERAL jsonb_each_text(wm.candidate_group_mappings)
WHERE wm.process_definition_key = 'oneCmsEnhancedWorkflow'
ORDER BY key;

-- Verify enhanced queue task mappings
SELECT 
    'Enhanced Queue Tasks:' as check_type,
    task_definition_key,
    queue_name, 
    candidate_group,
    task_type,
    priority_weight,
    sla_minutes,
    (metadata->>'user_actions')::jsonb as user_actions
FROM queue_tasks 
WHERE process_definition_key = 'oneCmsEnhancedWorkflow'
ORDER BY task_definition_key;

-- Verify case states and end events
SELECT 
    'Case States & End Events:' as check_type,
    jsonb_array_elements_text(metadata->'case_states') as case_states
FROM workflow_metadata 
WHERE process_definition_key = 'oneCmsEnhancedWorkflow'
UNION ALL
SELECT 
    'End Event Types:' as check_type,
    jsonb_array_elements_text(metadata->'end_event_types') as end_event_types
FROM workflow_metadata 
WHERE process_definition_key = 'oneCmsEnhancedWorkflow';

-- Verify user actions configuration
SELECT 
    'User Actions:' as check_type,
    key as action_category,
    value as available_actions
FROM workflow_metadata wm
CROSS JOIN LATERAL jsonb_each(wm.metadata->'user_actions')
WHERE wm.process_definition_key = 'oneCmsEnhancedWorkflow'
ORDER BY key;

-- Verify new tables created
SELECT 
    'New Tables Created:' as check_type,
    table_name,
    CASE WHEN table_name IS NOT NULL THEN 'EXISTS' ELSE 'MISSING' END as status
FROM information_schema.tables 
WHERE table_schema = CURRENT_SCHEMA() 
  AND table_name IN ('case_state_transitions', 'process_variable_audit');

COMMIT;