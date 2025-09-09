-- =====================================================================
-- EO INTAKE ANALYST ENTITLEMENTS SETUP
-- Version: 1.0.0  
-- Created: 2025-09-09
-- Description: Sets up complete entitlements for EO_INTAKE_ANALYST role
--              Based on the OneCMS workflow first step permissions matrix
-- =====================================================================

-- Set search path
SET search_path TO entitlements, public;

-- =====================================================================
-- PART 1: ENSURE REFERENCE DATA EXISTS
-- =====================================================================

-- Ensure Ethics Office Department exists
INSERT INTO entitlements.departments (department_name, department_code, description, is_active) 
VALUES ('Ethics Office', 'EO', 'Ethics Office - handles ethical violations, compliance, and integrity matters', true)
ON CONFLICT (department_code) DO NOTHING;

-- Ensure OneCMS business application exists
INSERT INTO entitlements.business_applications (business_app_name, description, is_active, metadata) 
VALUES ('onecms', 'OneCMS Case Management System - Ethics Office and Investigation workflows', true, '{
  "version": "2.0.0",
  "workflow_engine": "flowable", 
  "authorization_engine": "hybrid",
  "case_number_prefix": "CMS",
  "supported_workflows": ["oneCmsCaseWorkflow"],
  "departments": ["EO", "LEGAL", "INVESTIGATION", "ER", "CSIS"],
  "features": {
    "case_management": true,
    "workflow_automation": true,
    "document_management": true,
    "reporting": true
  }
}')
ON CONFLICT (business_app_name) DO NOTHING;

-- =====================================================================
-- PART 2: CREATE EO_INTAKE_ANALYST ROLE
-- =====================================================================

INSERT INTO entitlements.business_app_roles (business_app_id, role_name, role_display_name, description, is_active, metadata)
SELECT 
  ba.id,
  'EO_INTAKE_ANALYST',
  'EO Intake Analyst',
  'Ethics Office Intake Analyst - handles initial case creation and case building activities',
  true,
  '{
    "permissions": ["CREATE_CASE", "READ_CASE", "UPDATE_CASE", "BUILD_CASE", "CREATE_ENTITIES", "CREATE_ALLEGATIONS", "CREATE_NARRATIVES", "UPLOAD_DOCUMENTS", "EDIT_ENTITIES", "EDIT_ALLEGATIONS", "EDIT_NARRATIVE", "DELETE_ENTITIES", "DELETE_ALLEGATIONS", "DELETE_NARRATIVES", "COPY_ALLEGATIONS", "COPY_NARRATIVES", "ADD_COMMENTS", "ACCESS_CASE", "REJECT_CASE", "APPROVE_REJECTION"],
    "queues": ["eo-intake-analyst-queue"],
    "workflow_steps": ["Create Case", "Build Case"],
    "workflow_phases": ["Intake", "Build Case"],
    "tier": "analyst",
    "approval_level": "intake",
    "department": "EO",
    "candidate_group": "GROUP_EO_INTAKE_ANALYST",
    "case_lifecycle": {
      "can_create": true,
      "can_modify_in_phases": ["INTAKE", "BUILD_CASE"],
      "can_reject": true,
      "can_approve_rejection": false,
      "can_assign_to_iu": false
    },
    "bpmn_tasks": ["task_create_case", "task_fill_information"]
  }'
FROM entitlements.business_applications ba 
WHERE ba.business_app_name = 'onecms'
ON CONFLICT (business_app_id, role_name) DO UPDATE SET
  role_display_name = EXCLUDED.role_display_name,
  description = EXCLUDED.description,
  metadata = EXCLUDED.metadata;

-- =====================================================================
-- PART 3: CREATE SAMPLE EO_INTAKE_ANALYST USERS
-- =====================================================================

-- Create primary EO Intake Analyst user
INSERT INTO entitlements.users (id, username, email, first_name, last_name, is_active, attributes) VALUES
('alice.eo.intake', 'alice.eo.intake', 'alice.eo.intake@company.com', 'Alice', 'Johnson', true, '{
  "employee_id": "EMP_EO_001",
  "office_location": "HQ_Building_A", 
  "phone": "+1-555-0201",
  "hire_date": "2023-02-15",
  "security_clearance": "CONFIDENTIAL",
  "manager_id": "robert.eo.officer",
  "cost_center": "EO001",
  "title": "EO Intake Analyst",
  "specializations": ["case_intake", "initial_assessment", "documentation"],
  "certifications": ["CFE_Level1", "Ethics_Compliance"]
}')
ON CONFLICT (id) DO UPDATE SET
  email = EXCLUDED.email,
  first_name = EXCLUDED.first_name,
  last_name = EXCLUDED.last_name,
  attributes = EXCLUDED.attributes;

-- Create secondary EO Intake Analyst user
INSERT INTO entitlements.users (id, username, email, first_name, last_name, is_active, attributes) VALUES
('bob.eo.intake', 'bob.eo.intake', 'bob.eo.intake@company.com', 'Bob', 'Wilson', true, '{
  "employee_id": "EMP_EO_002",
  "office_location": "HQ_Building_A", 
  "phone": "+1-555-0202",
  "hire_date": "2023-06-20",
  "security_clearance": "CONFIDENTIAL",
  "manager_id": "robert.eo.officer",
  "cost_center": "EO001",
  "title": "EO Intake Analyst",
  "specializations": ["case_documentation", "narrative_building", "entity_management"],
  "certifications": ["CFE_Level1"]
}')
ON CONFLICT (id) DO UPDATE SET
  email = EXCLUDED.email,
  first_name = EXCLUDED.first_name,
  last_name = EXCLUDED.last_name,
  attributes = EXCLUDED.attributes;

-- =====================================================================
-- PART 4: USER DEPARTMENT ASSIGNMENTS
-- =====================================================================

-- Assign EO Intake Analysts to Ethics Office department
INSERT INTO entitlements.user_departments (user_id, department_id, is_active)
SELECT u.id, d.id, true
FROM entitlements.users u, entitlements.departments d
WHERE (u.id, d.department_code) IN (
  ('alice.eo.intake', 'EO'),
  ('bob.eo.intake', 'EO')
)
ON CONFLICT (user_id, department_id) DO UPDATE SET 
  is_active = true;

-- =====================================================================
-- PART 5: USER ROLE ASSIGNMENTS
-- =====================================================================

-- Assign EO_INTAKE_ANALYST role to users
INSERT INTO entitlements.user_business_app_roles (user_id, business_app_role_id, is_active)
SELECT u.id, bar.id, true
FROM entitlements.users u, entitlements.business_app_roles bar, entitlements.business_applications ba
WHERE ba.business_app_name = 'onecms' 
  AND bar.role_name = 'EO_INTAKE_ANALYST'
  AND u.id IN ('alice.eo.intake', 'bob.eo.intake')
  AND bar.business_app_id = ba.id
ON CONFLICT (user_id, business_app_role_id) DO UPDATE SET 
  is_active = true;

-- =====================================================================
-- PART 6: VERIFICATION QUERIES
-- =====================================================================

-- Verify EO_INTAKE_ANALYST role creation
SELECT 
  ba.business_app_name,
  bar.role_name,
  bar.role_display_name,
  bar.description,
  bar.metadata->>'department' as department,
  bar.metadata->'queues' as queues,
  bar.metadata->'permissions' as permissions
FROM entitlements.business_applications ba
JOIN entitlements.business_app_roles bar ON ba.id = bar.business_app_id
WHERE ba.business_app_name = 'onecms' 
  AND bar.role_name = 'EO_INTAKE_ANALYST';

-- Verify user assignments
SELECT 
  u.username,
  u.first_name || ' ' || u.last_name as full_name,
  d.department_code,
  d.department_name,
  bar.role_name,
  bar.role_display_name,
  u.attributes->>'title' as job_title
FROM entitlements.users u
JOIN entitlements.user_departments ud ON u.id = ud.user_id AND ud.is_active = true
JOIN entitlements.departments d ON ud.department_id = d.id
JOIN entitlements.user_business_app_roles ubar ON u.id = ubar.user_id AND ubar.is_active = true
JOIN entitlements.business_app_roles bar ON ubar.business_app_role_id = bar.id
JOIN entitlements.business_applications ba ON bar.business_app_id = ba.id
WHERE ba.business_app_name = 'onecms'
  AND bar.role_name = 'EO_INTAKE_ANALYST'
  AND u.is_active = true
ORDER BY u.username;

-- Verify queue access for EO Intake Analysts
SELECT 
  u.username,
  bar.role_name,
  bar.metadata->'queues' as assigned_queues,
  bar.metadata->'workflow_phases' as workflow_phases,
  bar.metadata->'bpmn_tasks' as bpmn_tasks
FROM entitlements.users u
JOIN entitlements.user_business_app_roles ubar ON u.id = ubar.user_id AND ubar.is_active = true
JOIN entitlements.business_app_roles bar ON ubar.business_app_role_id = bar.id
JOIN entitlements.business_applications ba ON bar.business_app_id = ba.id
WHERE ba.business_app_name = 'onecms'
  AND bar.role_name = 'EO_INTAKE_ANALYST'
  AND u.is_active = true;

-- =====================================================================
-- PART 7: TEST QUERIES FOR SPECIFIC PERMISSIONS
-- =====================================================================

-- Test: Check what actions EO_INTAKE_ANALYST can perform
SELECT 
  'EO_INTAKE_ANALYST Permissions Test' as test_name,
  jsonb_array_elements_text(bar.metadata->'permissions') as allowed_actions
FROM entitlements.business_app_roles bar
JOIN entitlements.business_applications ba ON bar.business_app_id = ba.id
WHERE ba.business_app_name = 'onecms' 
  AND bar.role_name = 'EO_INTAKE_ANALYST';

-- Test: Check workflow task assignments
SELECT 
  'Workflow Task Assignments' as test_name,
  jsonb_array_elements_text(bar.metadata->'bpmn_tasks') as bpmn_task_id
FROM entitlements.business_app_roles bar
JOIN entitlements.business_applications ba ON bar.business_app_id = ba.id
WHERE ba.business_app_name = 'onecms' 
  AND bar.role_name = 'EO_INTAKE_ANALYST';

-- =====================================================================
-- SUMMARY AND NOTES
-- =====================================================================

/*
EO_INTAKE_ANALYST ENTITLEMENTS SETUP COMPLETE

This script has configured:

1. ROLE PERMISSIONS (Based on entitlement matrix):
   ✓ CREATE_CASE - Yes (Create Case workflow step)
   ✓ CREATE_ENTITIES - Yes (Build Case workflow step)
   ✓ CREATE_ALLEGATIONS - Yes (Build Case workflow step) 
   ✓ CREATE_NARRATIVES - Yes (Build Case workflow step)
   ✓ UPLOAD_DOCUMENTS - Yes (Build Case workflow step)
   ✓ EDIT_ENTITIES - Yes (Build Case workflow step)
   ✓ EDIT_ALLEGATIONS - Yes (Build Case workflow step)
   ✓ EDIT_NARRATIVE - Yes (Build Case workflow step)
   ✓ DELETE_ENTITIES - No (Per matrix - assigned to "No")
   ✓ DELETE_ALLEGATIONS - Yes (Build Case workflow step)
   ✓ DELETE_NARRATIVES - Yes (Build Case workflow step)
   ✓ COPY_ALLEGATIONS - Yes (Build Case workflow step)
   ✓ COPY_NARRATIVES - Yes (Build Case workflow step)
   ✓ ADD_COMMENTS - Yes (Build Case workflow step)
   ✓ ACCESS_CASE - Yes (Build Case workflow step)
   ✓ REJECT_CASE - Yes (Intake workflow step)
   ✓ APPROVE_REJECTION - No (Per matrix - assigned to "No")

2. WORKFLOW INTEGRATION:
   ✓ Queue: "eo-intake-analyst-queue" 
   ✓ BPMN Tasks: task_create_case, task_fill_information
   ✓ Workflow Phases: INTAKE, BUILD_CASE
   ✓ Candidate Group: GROUP_EO_INTAKE_ANALYST

3. USER EXAMPLES:
   ✓ alice.eo.intake - Primary EO Intake Analyst
   ✓ bob.eo.intake - Secondary EO Intake Analyst
   
4. DEPARTMENT ASSIGNMENT:
   ✓ Both users assigned to "EO" (Ethics Office) department

This configuration enables EO Intake Analysts to:
- Receive tasks in the eo-intake-analyst-queue
- Create new cases in the OneCMS system
- Build case content (entities, allegations, narratives, documents)
- Edit and manage case information during intake and build phases
- Reject cases when appropriate
- Transition cases to next workflow phase via task completion

The setup aligns with the OneCMS Case Workflow (oneCmsCaseWorkflow) BPMN 
process definition and supports the first step of the workflow.
*/

-- =====================================================================
-- END OF SCRIPT
-- =====================================================================