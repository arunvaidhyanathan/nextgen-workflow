-- Workflow Metadata Registration Script
-- This script registers the OneCMS Unified Workflow with complete candidate group to queue mappings
-- Execute this script after deploying the BPMN and setting up the database schema

-- =======================================
-- 1. REGISTER ONECMS UNIFIED WORKFLOW METADATA
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
    'oneCmsUnifiedWorkflow',
    'OneCMS Unified Case Workflow',
    'OneCMS',
    '1.0',
    'Comprehensive case management workflow supporting EO and CSIS initiated cases with multi-department routing',
    -- Complete candidate group to queue mappings including new EO_INTAKE_ANALYST
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
        "entry_points": ["EO_INITIATED", "CSIS_INITIATED"],
        "departments": ["EO", "CSIS", "ER", "LEGAL", "INVESTIGATION"],
        "bpmn_file": "OneCMS_Nextgen_WF.bpmn20.xml",
        "form_keys": {
            "task_eo_intake_create": "eo-intake-review-form",
            "task_eo_head_review": "eo-head-review-form",
            "task_eo_officer_triage": "eo-officer-triage-form",
            "task_csis_manager_create": "csis-manager-create-form",
            "task_csis_analyst_vet": "csis-analyst-vet-form",
            "task_er_intake": "er-intake-form",
            "task_legal_intake": "legal-intake-form",
            "task_inv_manager_assign": "inv-manager-assign-form",
            "task_investigator_execute": "investigator-execute-form"
        },
        "workflow_personas": [
            {
                "role": "GROUP_EO_INTAKE_ANALYST",
                "description": "Ethics Office Intake Analyst - Initial case creation and intake",
                "department": "EO",
                "queue": "eo-intake-analyst-queue",
                "responsibilities": ["create_case", "initial_intake", "case_routing"]
            },
            {
                "role": "GROUP_EO_HEAD", 
                "description": "Ethics Office Head - Strategic review and approval",
                "department": "EO",
                "queue": "eo-head-queue",
                "responsibilities": ["strategic_review", "case_approval", "escalation_handling"]
            },
            {
                "role": "GROUP_EO_OFFICER",
                "description": "Ethics Office Officer - Triage and departmental routing",
                "department": "EO", 
                "queue": "eo-officer-queue",
                "responsibilities": ["case_triage", "department_routing", "case_closure"]
            },
            {
                "role": "GROUP_CSIS_INTAKE_MANAGER",
                "description": "CSIS Intake Manager - Security case management",
                "department": "CSIS",
                "queue": "csis-intake-manager-queue", 
                "responsibilities": ["security_case_review", "analyst_assignment", "case_approval"]
            },
            {
                "role": "GROUP_CSIS_INTAKE_ANALYST",
                "description": "CSIS Intake Analyst - Security analysis and vetting", 
                "department": "CSIS",
                "queue": "csis-intake-analyst-queue",
                "responsibilities": ["security_analysis", "case_vetting", "threat_assessment"]
            },
            {
                "role": "GROUP_ER_INTAKE_ANALYST",
                "description": "Employee Relations Intake Analyst - HR case processing",
                "department": "ER",
                "queue": "er-intake-analyst-queue",
                "responsibilities": ["hr_case_intake", "employee_relations", "policy_compliance"]
            },
            {
                "role": "GROUP_LEGAL_INTAKE_ANALYST", 
                "description": "Legal Intake Analyst - Legal case evaluation",
                "department": "LEGAL",
                "queue": "legal-intake-analyst-queue",
                "responsibilities": ["legal_review", "compliance_check", "regulatory_assessment"]
            },
            {
                "role": "GROUP_INVESTIGATION_MANAGER",
                "description": "Investigation Manager - Investigation oversight",
                "department": "INVESTIGATION", 
                "queue": "investigation-manager-queue",
                "responsibilities": ["investigator_assignment", "case_oversight", "quality_control"]
            },
            {
                "role": "GROUP_INVESTIGATOR",
                "description": "Investigator - Case investigation execution",
                "department": "INVESTIGATION",
                "queue": "investigator-queue", 
                "responsibilities": ["case_investigation", "evidence_collection", "report_creation"]
            }
        ]
    }'::jsonb
)
ON CONFLICT (process_definition_key, business_app_name) 
DO UPDATE SET
    process_definition_name = EXCLUDED.process_definition_name,
    candidate_group_mappings = EXCLUDED.candidate_group_mappings,
    deployment_status = EXCLUDED.deployment_status,
    updated_at = CURRENT_TIMESTAMP,
    metadata = EXCLUDED.metadata;

-- =======================================
-- 2. REGISTER QUEUE TASK MAPPINGS
-- =======================================

-- Clear existing mappings for this workflow to prevent duplicates
DELETE FROM queue_tasks 
WHERE process_definition_key = 'oneCmsUnifiedWorkflow'
  AND business_app_name = 'OneCMS';

-- Insert comprehensive task-to-queue mappings
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

-- EO Intake Tasks
('oneCmsUnifiedWorkflow', 'OneCMS', 'task_eo_intake_create', 'eo-intake-analyst-queue', 'GROUP_EO_INTAKE_ANALYST', 'INTAKE', 80, 240, 
 '{"escalation_levels": [{"level": 1, "minutes": 240, "notify": ["supervisor"]}, {"level": 2, "minutes": 480, "notify": ["manager"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "eo-intake-review-form", "department": "EO"}'::jsonb),

-- EO Head Tasks  
('oneCmsUnifiedWorkflow', 'OneCMS', 'task_eo_head_review', 'eo-head-queue', 'GROUP_EO_HEAD', 'REVIEW', 100, 480,
 '{"escalation_levels": [{"level": 1, "minutes": 480, "notify": ["director"]}, {"level": 2, "minutes": 960, "notify": ["executive"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "eo-head-review-form", "department": "EO"}'::jsonb),

-- EO Officer Tasks
('oneCmsUnifiedWorkflow', 'OneCMS', 'task_eo_officer_triage', 'eo-officer-queue', 'GROUP_EO_OFFICER', 'TRIAGE', 90, 360,
 '{"escalation_levels": [{"level": 1, "minutes": 360, "notify": ["supervisor"]}, {"level": 2, "minutes": 720, "notify": ["head"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "eo-officer-triage-form", "department": "EO"}'::jsonb),

-- CSIS Manager Tasks
('oneCmsUnifiedWorkflow', 'OneCMS', 'task_csis_manager_create', 'csis-intake-manager-queue', 'GROUP_CSIS_INTAKE_MANAGER', 'CREATE', 85, 300,
 '{"escalation_levels": [{"level": 1, "minutes": 300, "notify": ["director"]}, {"level": 2, "minutes": 600, "notify": ["security_head"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "csis-manager-create-form", "department": "CSIS"}'::jsonb),

('oneCmsUnifiedWorkflow', 'OneCMS', 'task_csis_manager_review', 'csis-intake-manager-queue', 'GROUP_CSIS_INTAKE_MANAGER', 'REVIEW', 85, 240,
 '{"escalation_levels": [{"level": 1, "minutes": 240, "notify": ["director"]}, {"level": 2, "minutes": 480, "notify": ["security_head"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "csis-manager-review-form", "department": "CSIS"}'::jsonb),

-- CSIS Analyst Tasks
('oneCmsUnifiedWorkflow', 'OneCMS', 'task_csis_analyst_vet', 'csis-intake-analyst-queue', 'GROUP_CSIS_INTAKE_ANALYST', 'ANALYSIS', 70, 480,
 '{"escalation_levels": [{"level": 1, "minutes": 480, "notify": ["manager"]}, {"level": 2, "minutes": 960, "notify": ["director"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "csis-analyst-vet-form", "department": "CSIS"}'::jsonb),

-- Employee Relations Tasks
('oneCmsUnifiedWorkflow', 'OneCMS', 'task_er_intake', 'er-intake-analyst-queue', 'GROUP_ER_INTAKE_ANALYST', 'INTAKE', 75, 360,
 '{"escalation_levels": [{"level": 1, "minutes": 360, "notify": ["hr_manager"]}, {"level": 2, "minutes": 720, "notify": ["hr_director"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "er-intake-form", "department": "ER"}'::jsonb),

-- Legal Tasks
('oneCmsUnifiedWorkflow', 'OneCMS', 'task_legal_intake', 'legal-intake-analyst-queue', 'GROUP_LEGAL_INTAKE_ANALYST', 'LEGAL_REVIEW', 90, 720,
 '{"escalation_levels": [{"level": 1, "minutes": 720, "notify": ["legal_manager"]}, {"level": 2, "minutes": 1440, "notify": ["general_counsel"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "legal-intake-form", "department": "LEGAL"}'::jsonb),

-- Investigation Manager Tasks  
('oneCmsUnifiedWorkflow', 'OneCMS', 'task_inv_manager_assign', 'investigation-manager-queue', 'GROUP_INVESTIGATION_MANAGER', 'ASSIGNMENT', 95, 240,
 '{"escalation_levels": [{"level": 1, "minutes": 240, "notify": ["director"]}, {"level": 2, "minutes": 480, "notify": ["chief_investigator"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "inv-manager-assign-form", "department": "INVESTIGATION"}'::jsonb),

-- Investigator Tasks
('oneCmsUnifiedWorkflow', 'OneCMS', 'task_investigator_execute', 'investigator-queue', 'GROUP_INVESTIGATOR', 'INVESTIGATION', 60, 2880,
 '{"escalation_levels": [{"level": 1, "minutes": 2880, "notify": ["manager"]}, {"level": 2, "minutes": 5760, "notify": ["director"]}]}'::jsonb,
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{"form_key": "investigator-execute-form", "department": "INVESTIGATION"}'::jsonb);

-- =======================================
-- 3. VERIFY THE REGISTRATION
-- =======================================

-- Verify workflow metadata registration
SELECT 
    'Workflow Metadata:' as check_type,
    process_definition_key,
    process_definition_name,
    business_app_name,
    deployment_status,
    jsonb_array_length(candidate_group_mappings::jsonb) as mapping_count
FROM workflow_metadata 
WHERE process_definition_key = 'oneCmsUnifiedWorkflow';

-- Verify candidate group mappings
SELECT 
    'Candidate Mappings:' as check_type,
    key as candidate_group,
    value as queue_name
FROM workflow_metadata wm
CROSS JOIN LATERAL jsonb_each_text(wm.candidate_group_mappings)
WHERE wm.process_definition_key = 'oneCmsUnifiedWorkflow'
ORDER BY key;

-- Verify queue task mappings
SELECT 
    'Queue Tasks:' as check_type,
    task_definition_key,
    queue_name, 
    candidate_group,
    task_type,
    priority_weight,
    sla_minutes
FROM queue_tasks 
WHERE process_definition_key = 'oneCmsUnifiedWorkflow'
ORDER BY task_definition_key;

-- Verify queue existence for all mappings
SELECT 
    'Queue Validation:' as check_type,
    wq.queue_name,
    wq.queue_type,
    wq.is_active,
    CASE WHEN wq.queue_name IS NULL THEN 'MISSING' ELSE 'EXISTS' END as status
FROM (
    SELECT DISTINCT value as queue_name
    FROM workflow_metadata wm
    CROSS JOIN LATERAL jsonb_each_text(wm.candidate_group_mappings)
    WHERE wm.process_definition_key = 'oneCmsUnifiedWorkflow'
) expected_queues
LEFT JOIN workflow_queues wq ON expected_queues.queue_name = wq.queue_name
ORDER BY expected_queues.queue_name;

COMMIT;