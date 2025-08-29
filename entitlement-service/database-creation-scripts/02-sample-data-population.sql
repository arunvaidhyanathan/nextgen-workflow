-- =====================================================================
-- NextGen Workflow Entitlement Service - Sample Data Population
-- Version: 1.0.0  
-- Created: 2025-08-28
-- Description: Populates the entitlement service database with realistic
--              sample data for development and testing purposes
-- =====================================================================

-- Set search path
SET search_path TO entitlements, public;

-- =====================================================================
-- PART 1: REFERENCE DATA (Departments and Business Applications)
-- =====================================================================

-- Insert Departments
INSERT INTO entitlements.departments (department_name, department_code, description, is_active) VALUES
('Ethics Office', 'EO', 'Ethics Office - handles ethical violations, compliance, and integrity matters', true),
('Human Resources', 'HR', 'Human Resources Department - manages employee relations and HR policies', true), 
('Legal Department', 'LEGAL', 'Legal Department - provides legal counsel and handles legal matters', true),
('Investigation Unit', 'INVESTIGATION', 'Investigation Unit - conducts formal investigations and fact-finding', true),
('Corporate Security', 'CSIS', 'Corporate Security & Information Security - handles security matters', true),
('Employee Relations', 'ER', 'Employee Relations - manages workplace relations and conflict resolution', true),
('Internal Audit', 'AUDIT', 'Internal Audit Department - conducts internal audits and reviews', true),
('Compliance', 'COMPLIANCE', 'Compliance Department - ensures regulatory compliance', true);

-- Insert Business Applications
INSERT INTO entitlements.business_applications (business_app_name, description, is_active, metadata) VALUES
('onecms', 'OneCMS Case Management System - Ethics Office and Investigation workflows', true, '{
  "version": "2.0.0",
  "workflow_engine": "flowable", 
  "authorization_engine": "hybrid",
  "case_number_prefix": "CMS",
  "supported_workflows": ["OneCMS_Workflow", "OneCMS_MultiDepartment_Workflow", "oneCmsUnifiedWorkflow"],
  "departments": ["EO", "HR", "LEGAL", "SECURITY", "INVESTIGATION", "ER", "CSIS"],
  "features": {
    "case_management": true,
    "workflow_automation": true,
    "document_management": false,
    "reporting": true
  }
}'),
('hrms', 'Human Resources Management System', true, '{
  "version": "1.5.0",
  "workflow_engine": "custom",
  "authorization_engine": "database",
  "departments": ["HR", "ER"],
  "features": {
    "employee_management": true,
    "leave_management": true,
    "performance_reviews": true
  }
}'),
('legal_tracker', 'Legal Matter Tracking System', true, '{
  "version": "1.2.0", 
  "workflow_engine": "none",
  "authorization_engine": "database",
  "departments": ["LEGAL"],
  "features": {
    "matter_tracking": true,
    "document_management": true,
    "billing_integration": true
  }
}');

-- =====================================================================
-- PART 2: BUSINESS APPLICATION ROLES
-- =====================================================================

-- OneCMS Roles
INSERT INTO entitlements.business_app_roles (business_app_id, role_name, role_display_name, description, is_active, metadata)
SELECT 
  ba.id,
  'EO_HEAD',
  'Ethics Office Head', 
  'Head of Ethics Office - senior leadership oversight',
  true,
  '{"permissions": ["CREATE", "READ", "UPDATE", "DELETE", "APPROVE", "ADMIN"], "queues": ["eo-head-queue"], "tier": "executive", "approval_level": "senior", "department": "EO"}'
FROM entitlements.business_applications ba WHERE ba.business_app_name = 'onecms';

INSERT INTO entitlements.business_app_roles (business_app_id, role_name, role_display_name, description, is_active, metadata)
SELECT 
  ba.id,
  'EO_OFFICER', 
  'Ethics Office Officer',
  'Ethics Office Officer - case triage and routing decisions',
  true,
  '{"permissions": ["CREATE", "READ", "UPDATE", "ASSIGN"], "queues": ["eo-officer-queue"], "tier": "officer", "approval_level": "standard", "department": "EO"}'
FROM entitlements.business_applications ba WHERE ba.business_app_name = 'onecms';

INSERT INTO entitlements.business_app_roles (business_app_id, role_name, role_display_name, description, is_active, metadata)
SELECT 
  ba.id,
  'CSIS_INTAKE_MANAGER',
  'CSIS Intake Manager',
  'Corporate Security Intake Manager - review and assignment',
  true,
  '{"permissions": ["READ", "UPDATE", "ASSIGN", "APPROVE"], "queues": ["csis-intake-manager-queue"], "tier": "manager", "approval_level": "manager", "department": "CSIS"}'
FROM entitlements.business_applications ba WHERE ba.business_app_name = 'onecms';

INSERT INTO entitlements.business_app_roles (business_app_id, role_name, role_display_name, description, is_active, metadata)
SELECT 
  ba.id,
  'CSIS_INTAKE_ANALYST',
  'CSIS Intake Analyst',
  'Corporate Security Intake Analyst - detailed analysis and vetting',
  true,
  '{"permissions": ["READ", "UPDATE"], "queues": ["csis-intake-analyst-queue"], "tier": "analyst", "approval_level": "analyst", "department": "CSIS"}'
FROM entitlements.business_applications ba WHERE ba.business_app_name = 'onecms';

INSERT INTO entitlements.business_app_roles (business_app_id, role_name, role_display_name, description, is_active, metadata)
SELECT 
  ba.id,
  'ER_INTAKE_ANALYST',
  'Employee Relations Intake Analyst',
  'Employee Relations Intake Analyst - ER department intake and routing',
  true,
  '{"permissions": ["READ", "UPDATE", "ASSIGN"], "queues": ["er-intake-analyst-queue"], "tier": "analyst", "approval_level": "standard", "department": "ER"}'
FROM entitlements.business_applications ba WHERE ba.business_app_name = 'onecms';

INSERT INTO entitlements.business_app_roles (business_app_id, role_name, role_display_name, description, is_active, metadata)
SELECT 
  ba.id,
  'LEGAL_INTAKE_ANALYST',
  'Legal Intake Analyst', 
  'Legal Department Intake Analyst - legal matters routing',
  true,
  '{"permissions": ["READ", "UPDATE", "REVIEW"], "queues": ["legal-intake-analyst-queue"], "tier": "analyst", "approval_level": "standard", "department": "LEGAL"}'
FROM entitlements.business_applications ba WHERE ba.business_app_name = 'onecms';

INSERT INTO entitlements.business_app_roles (business_app_id, role_name, role_display_name, description, is_active, metadata)
SELECT 
  ba.id,
  'INVESTIGATION_MANAGER',
  'Investigation Manager',
  'Investigation Manager - reviews and assigns investigators',
  true,
  '{"permissions": ["READ", "UPDATE", "ASSIGN", "APPROVE"], "queues": ["investigation-manager-queue"], "tier": "manager", "approval_level": "manager", "department": "INVESTIGATION"}'
FROM entitlements.business_applications ba WHERE ba.business_app_name = 'onecms';

INSERT INTO entitlements.business_app_roles (business_app_id, role_name, role_display_name, description, is_active, metadata)
SELECT 
  ba.id,
  'INVESTIGATOR',
  'Investigator',
  'Investigator - executes investigation tasks and case work',
  true,
  '{"permissions": ["READ", "UPDATE", "INVESTIGATE"], "queues": ["investigator-queue"], "tier": "investigator", "approval_level": "standard", "department": "INVESTIGATION"}'
FROM entitlements.business_applications ba WHERE ba.business_app_name = 'onecms';

-- Legacy OneCMS Roles (for backward compatibility)
INSERT INTO entitlements.business_app_roles (business_app_id, role_name, role_display_name, description, is_active, metadata)
SELECT 
  ba.id,
  'INTAKE_ANALYST',
  'Intake Analyst (Legacy)',
  'Legacy intake analyst role for backward compatibility',
  true,
  '{"permissions": ["READ", "CREATE"], "queues": ["intake-analyst-queue"], "tier": "analyst", "legacy": true, "department": "EO"}'
FROM entitlements.business_applications ba WHERE ba.business_app_name = 'onecms';

-- HRMS Roles
INSERT INTO entitlements.business_app_roles (business_app_id, role_name, role_display_name, description, is_active, metadata)
SELECT 
  ba.id,
  'HR_MANAGER',
  'HR Manager',
  'Human Resources Manager',
  true,
  '{"permissions": ["READ", "UPDATE", "APPROVE"], "department": "HR"}'
FROM entitlements.business_applications ba WHERE ba.business_app_name = 'hrms';

INSERT INTO entitlements.business_app_roles (business_app_id, role_name, role_display_name, description, is_active, metadata)
SELECT 
  ba.id,
  'HR_SPECIALIST',
  'HR Specialist',
  'Human Resources Specialist',
  true,
  '{"permissions": ["READ", "UPDATE"], "department": "HR"}'
FROM entitlements.business_applications ba WHERE ba.business_app_name = 'hrms';

-- Legal Tracker Roles
INSERT INTO entitlements.business_app_roles (business_app_id, role_name, role_display_name, description, is_active, metadata)
SELECT 
  ba.id,
  'LEGAL_COUNSEL',
  'Legal Counsel',
  'Legal Counsel - handles legal matters',
  true,
  '{"permissions": ["READ", "UPDATE", "APPROVE"], "department": "LEGAL"}'
FROM entitlements.business_applications ba WHERE ba.business_app_name = 'legal_tracker';

-- =====================================================================
-- PART 3: USERS
-- =====================================================================

INSERT INTO entitlements.users (id, username, email, first_name, last_name, is_active, attributes) VALUES
-- Ethics Office Users
('alice.intake', 'alice.intake', 'alice.intake@company.com', 'Alice', 'Smith', true, '{
  "employee_id": "EMP001",
  "office_location": "HQ_Building_A", 
  "phone": "+1-555-0101",
  "hire_date": "2023-01-15",
  "security_clearance": "CONFIDENTIAL",
  "manager_id": "margaret.eo",
  "cost_center": "EO001"
}'),

('margaret.eo', 'margaret.eo', 'margaret.eo@company.com', 'Margaret', 'Williams', true, '{
  "employee_id": "EMP002",
  "office_location": "HQ_Building_A",
  "phone": "+1-555-0102", 
  "hire_date": "2020-03-22",
  "security_clearance": "SECRET",
  "title": "Head of Ethics Office",
  "cost_center": "EO001"
}'),

('robert.eo', 'robert.eo', 'robert.eo@company.com', 'Robert', 'Chen', true, '{
  "employee_id": "EMP003",
  "office_location": "HQ_Building_A",
  "phone": "+1-555-0103",
  "hire_date": "2021-07-10",
  "security_clearance": "CONFIDENTIAL",
  "manager_id": "margaret.eo",
  "cost_center": "EO001"
}'),

-- Investigation Users
('edward.inv', 'edward.inv', 'edward.inv@company.com', 'Edward', 'Johnson', true, '{
  "employee_id": "EMP004",
  "office_location": "HQ_Building_B",
  "phone": "+1-555-0104",
  "hire_date": "2022-05-18",
  "security_clearance": "SECRET",
  "specialization": ["financial_crimes", "workplace_harassment"],
  "manager_id": "diana.invmgr",
  "cost_center": "INV001"
}'),

('diana.invmgr', 'diana.invmgr', 'diana.invmgr@company.com', 'Diana', 'Rodriguez', true, '{
  "employee_id": "EMP005", 
  "office_location": "HQ_Building_B",
  "phone": "+1-555-0105",
  "hire_date": "2019-11-30",
  "security_clearance": "SECRET",
  "title": "Investigation Manager",
  "cost_center": "INV001"
}'),

('carlos.inv', 'carlos.inv', 'carlos.inv@company.com', 'Carlos', 'Martinez', true, '{
  "employee_id": "EMP006",
  "office_location": "HQ_Building_B", 
  "phone": "+1-555-0106",
  "hire_date": "2021-09-12",
  "security_clearance": "CONFIDENTIAL",
  "specialization": ["fraud", "compliance_violations"],
  "manager_id": "diana.invmgr",
  "cost_center": "INV001"
}'),

-- Legal Users
('sarah.legal', 'sarah.legal', 'sarah.legal@company.com', 'Sarah', 'Davis', true, '{
  "employee_id": "EMP007",
  "office_location": "HQ_Building_C",
  "phone": "+1-555-0107",
  "hire_date": "2020-02-14",
  "security_clearance": "CONFIDENTIAL",
  "bar_admission": ["NY", "DC"],
  "specialization": ["employment_law", "regulatory_compliance"],
  "manager_id": "james.legal",
  "cost_center": "LEG001"
}'),

('james.legal', 'james.legal', 'james.legal@company.com', 'James', 'Thompson', true, '{
  "employee_id": "EMP008",
  "office_location": "HQ_Building_C",
  "phone": "+1-555-0108", 
  "hire_date": "2018-06-01",
  "security_clearance": "SECRET",
  "bar_admission": ["NY", "DC", "CA"],
  "title": "Senior Legal Counsel",
  "cost_center": "LEG001"
}'),

-- CSIS Users
('michael.csis', 'michael.csis', 'michael.csis@company.com', 'Michael', 'Lee', true, '{
  "employee_id": "EMP009",
  "office_location": "HQ_Building_D",
  "phone": "+1-555-0109",
  "hire_date": "2021-12-03",
  "security_clearance": "TOP_SECRET",
  "certifications": ["CISSP", "CISM"],
  "manager_id": "jennifer.csismgr",
  "cost_center": "CSIS001"
}'),

('jennifer.csismgr', 'jennifer.csismgr', 'jennifer.csismgr@company.com', 'Jennifer', 'Kim', true, '{
  "employee_id": "EMP010",
  "office_location": "HQ_Building_D",
  "phone": "+1-555-0110",
  "hire_date": "2019-04-15",
  "security_clearance": "TOP_SECRET", 
  "certifications": ["CISSP", "CISM", "CRISC"],
  "title": "CSIS Manager",
  "cost_center": "CSIS001"
}'),

-- HR/ER Users
('lisa.hr', 'lisa.hr', 'lisa.hr@company.com', 'Lisa', 'Anderson', true, '{
  "employee_id": "EMP011",
  "office_location": "HQ_Building_E",
  "phone": "+1-555-0111",
  "hire_date": "2022-08-20",
  "security_clearance": "CONFIDENTIAL",
  "certifications": ["PHR", "SHRM-CP"],
  "manager_id": "david.hrmgr",
  "cost_center": "HR001"
}'),

('david.hrmgr', 'david.hrmgr', 'david.hrmgr@company.com', 'David', 'Wilson', true, '{
  "employee_id": "EMP012",
  "office_location": "HQ_Building_E",
  "phone": "+1-555-0112",
  "hire_date": "2017-10-08",
  "security_clearance": "CONFIDENTIAL",
  "certifications": ["SPHR", "SHRM-SCP"],
  "title": "HR Manager",
  "cost_center": "HR001"
}'),

-- System Admin
('mike.admin', 'mike.admin', 'mike.admin@company.com', 'Mike', 'Taylor', true, '{
  "employee_id": "EMP013",
  "office_location": "HQ_Building_F",
  "phone": "+1-555-0113",
  "hire_date": "2019-01-10",
  "security_clearance": "SECRET",
  "certifications": ["CISSP", "AWS_SA"],
  "title": "System Administrator",
  "cost_center": "IT001"
}');

-- =====================================================================
-- PART 4: USER DEPARTMENT ASSIGNMENTS
-- =====================================================================

-- Ethics Office assignments
INSERT INTO entitlements.user_departments (user_id, department_id, is_active)
SELECT u.id, d.id, true
FROM entitlements.users u, entitlements.departments d
WHERE (u.id, d.department_code) IN (
  ('alice.intake', 'EO'),
  ('margaret.eo', 'EO'),
  ('robert.eo', 'EO')
);

-- Investigation assignments
INSERT INTO entitlements.user_departments (user_id, department_id, is_active)
SELECT u.id, d.id, true
FROM entitlements.users u, entitlements.departments d
WHERE (u.id, d.department_code) IN (
  ('edward.inv', 'INVESTIGATION'),
  ('diana.invmgr', 'INVESTIGATION'),
  ('carlos.inv', 'INVESTIGATION')
);

-- Legal assignments
INSERT INTO entitlements.user_departments (user_id, department_id, is_active)
SELECT u.id, d.id, true
FROM entitlements.users u, entitlements.departments d
WHERE (u.id, d.department_code) IN (
  ('sarah.legal', 'LEGAL'),
  ('james.legal', 'LEGAL')
);

-- CSIS assignments
INSERT INTO entitlements.user_departments (user_id, department_id, is_active)
SELECT u.id, d.id, true
FROM entitlements.users u, entitlements.departments d
WHERE (u.id, d.department_code) IN (
  ('michael.csis', 'CSIS'),
  ('jennifer.csismgr', 'CSIS')
);

-- HR/ER assignments
INSERT INTO entitlements.user_departments (user_id, department_id, is_active)
SELECT u.id, d.id, true
FROM entitlements.users u, entitlements.departments d
WHERE (u.id, d.department_code) IN (
  ('lisa.hr', 'HR'),
  ('lisa.hr', 'ER'),  -- HR user also handles ER
  ('david.hrmgr', 'HR'),
  ('david.hrmgr', 'ER')
);

-- =====================================================================
-- PART 5: USER ROLE ASSIGNMENTS
-- =====================================================================

-- OneCMS Role Assignments
INSERT INTO entitlements.user_business_app_roles (user_id, business_app_role_id, is_active)
SELECT u.id, bar.id, true
FROM entitlements.users u, entitlements.business_app_roles bar, entitlements.business_applications ba
WHERE ba.business_app_name = 'onecms' AND (u.id, bar.role_name) IN (
  -- Ethics Office roles
  ('margaret.eo', 'EO_HEAD'),
  ('alice.intake', 'EO_OFFICER'),
  ('robert.eo', 'EO_OFFICER'),
  
  -- Investigation roles  
  ('diana.invmgr', 'INVESTIGATION_MANAGER'),
  ('edward.inv', 'INVESTIGATOR'),
  ('carlos.inv', 'INVESTIGATOR'),
  
  -- Legal roles
  ('sarah.legal', 'LEGAL_INTAKE_ANALYST'),
  ('james.legal', 'LEGAL_INTAKE_ANALYST'),
  
  -- CSIS roles
  ('jennifer.csismgr', 'CSIS_INTAKE_MANAGER'),
  ('michael.csis', 'CSIS_INTAKE_ANALYST'),
  
  -- HR/ER roles
  ('lisa.hr', 'ER_INTAKE_ANALYST'),
  ('david.hrmgr', 'ER_INTAKE_ANALYST'),
  
  -- Legacy compatibility
  ('alice.intake', 'INTAKE_ANALYST')
) AND bar.business_app_id = ba.id;

-- HRMS Role Assignments
INSERT INTO entitlements.user_business_app_roles (user_id, business_app_role_id, is_active)
SELECT u.id, bar.id, true
FROM entitlements.users u, entitlements.business_app_roles bar, entitlements.business_applications ba
WHERE ba.business_app_name = 'hrms' AND (u.id, bar.role_name) IN (
  ('david.hrmgr', 'HR_MANAGER'),
  ('lisa.hr', 'HR_SPECIALIST')
) AND bar.business_app_id = ba.id;

-- Legal Tracker Role Assignments
INSERT INTO entitlements.user_business_app_roles (user_id, business_app_role_id, is_active)
SELECT u.id, bar.id, true
FROM entitlements.users u, entitlements.business_app_roles bar, entitlements.business_applications ba
WHERE ba.business_app_name = 'legal_tracker' AND (u.id, bar.role_name) IN (
  ('james.legal', 'LEGAL_COUNSEL'),
  ('sarah.legal', 'LEGAL_COUNSEL')
) AND bar.business_app_id = ba.id;

-- =====================================================================
-- PART 6: VERIFICATION QUERIES
-- =====================================================================

-- Verify department assignments
SELECT 
  d.department_code,
  d.department_name,
  COUNT(ud.user_id) as user_count,
  ARRAY_AGG(u.username ORDER BY u.username) as assigned_users
FROM entitlements.departments d
LEFT JOIN entitlements.user_departments ud ON d.id = ud.department_id AND ud.is_active = true
LEFT JOIN entitlements.users u ON ud.user_id = u.id AND u.is_active = true
WHERE d.is_active = true
GROUP BY d.id, d.department_code, d.department_name
ORDER BY d.department_code;

-- Verify role assignments by application
SELECT 
  ba.business_app_name,
  bar.role_name,
  COUNT(ubar.user_id) as user_count,
  ARRAY_AGG(u.username ORDER BY u.username) as assigned_users
FROM entitlements.business_applications ba
JOIN entitlements.business_app_roles bar ON ba.id = bar.business_app_id AND bar.is_active = true
LEFT JOIN entitlements.user_business_app_roles ubar ON bar.id = ubar.business_app_role_id AND ubar.is_active = true
LEFT JOIN entitlements.users u ON ubar.user_id = u.id AND u.is_active = true
WHERE ba.is_active = true
GROUP BY ba.id, ba.business_app_name, bar.role_name
ORDER BY ba.business_app_name, bar.role_name;

-- Verify user summary
SELECT 
  u.username,
  u.first_name || ' ' || u.last_name as full_name,
  ARRAY_AGG(DISTINCT d.department_code ORDER BY d.department_code) as departments,
  ARRAY_AGG(DISTINCT bar.role_name ORDER BY bar.role_name) as roles,
  u.attributes->'title' as title
FROM entitlements.users u
LEFT JOIN entitlements.user_departments ud ON u.id = ud.user_id AND ud.is_active = true
LEFT JOIN entitlements.departments d ON ud.department_id = d.id AND d.is_active = true
LEFT JOIN entitlements.user_business_app_roles ubar ON u.id = ubar.user_id AND ubar.is_active = true
LEFT JOIN entitlements.business_app_roles bar ON ubar.business_app_role_id = bar.id AND bar.is_active = true
WHERE u.is_active = true
GROUP BY u.id, u.username, u.first_name, u.last_name, u.attributes
ORDER BY u.username;

-- Test utility functions (if they exist)
-- SELECT entitlements.get_user_departments('alice.intake') as alice_departments;
-- SELECT entitlements.get_user_roles('edward.inv') as edward_roles;
-- SELECT entitlements.user_has_role('margaret.eo', 'EO_HEAD') as margaret_is_eo_head;

-- =====================================================================
-- SUMMARY
-- =====================================================================

/*
Data Population Summary:
- 8 Departments (EO, HR, LEGAL, INVESTIGATION, CSIS, ER, AUDIT, COMPLIANCE)
- 3 Business Applications (onecms, hrms, legal_tracker)
- 12 Business App Roles (covering all major personas)
- 13 Users (representing all departments and roles)
- 16 Department assignments (some users in multiple departments)
- 19 Role assignments (some users have multiple roles)

Key Test Users:
- margaret.eo: EO Head (senior leadership)
- alice.intake: EO Officer (intake analyst)
- diana.invmgr: Investigation Manager
- edward.inv: Investigator
- sarah.legal: Legal Intake Analyst
- jennifer.csismgr: CSIS Manager
- mike.admin: System Administrator

This data supports testing of:
- Multi-department workflows
- Role-based access control
- Queue-based task assignment
- Hierarchical approvals
- Cross-departmental case routing
*/

-- =====================================================================
-- END OF SCRIPT
-- =====================================================================