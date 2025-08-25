-- Seed Test Users for NextGen Workflow System
-- This script creates test users with proper password hashes and role assignments

-- Connect to the nextgen_workflow database and use the entitlements schema
\c nextgen_workflow;
SET search_path TO entitlements;

-- Insert test users with BCrypt hashed passwords (password123 for all)
-- BCrypt hash for 'password123': $2a$10$K8S0QlQYJ1YqZc5zq5hQOuXK.QJ8.jCp1/M8zCzJ2LqZq5hQOuXK.QJ
INSERT INTO users (id, username, password_hash, email, first_name, last_name, active, created_at, updated_at, attributes) VALUES
(1, 'alice.intake', '$2a$10$K8S0QlQYJ1YqZc5zq5hQOuXK.QJ8.jCp1/M8zCzJ2LqZq5hQOuXK.QJ', 'alice.intake@company.com', 'Alice', 'Johnson', true, NOW(), NOW(), '{"roles": ["intake_analyst", "user"], "department": "intake", "queue": "intake-analyst-queue"}'),
(2, 'edward.inv', '$2a$10$K8S0QlQYJ1YqZc5zq5hQOuXK.QJ8.jCp1/M8zCzJ2LqZq5hQOuXK.QJ', 'edward.inv@company.com', 'Edward', 'Smith', true, NOW(), NOW(), '{"roles": ["investigator", "user"], "department": "investigation", "queue": "investigation-queue"}'),
(3, 'sarah.legal', '$2a$10$K8S0QlQYJ1YqZc5zq5hQOuXK.QJ8.jCp1/M8zCzJ2LqZq5hQOuXK.QJ', 'sarah.legal@company.com', 'Sarah', 'Wilson', true, NOW(), NOW(), '{"roles": ["legal_counsel", "hr_legal", "user"], "department": "legal", "queue": "hr-legal-queue"}'),
(4, 'mike.admin', '$2a$10$K8S0QlQYJ1YqZc5zq5hQOuXK.QJ8.jCp1/M8zCzJ2LqZq5hQOuXK.QJ', 'mike.admin@company.com', 'Mike', 'Davis', true, NOW(), NOW(), '{"roles": ["admin", "system_admin", "user"], "department": "admin", "queue": "admin-queue"}'),
(5, 'jane.manager', '$2a$10$K8S0QlQYJ1YqZc5zq5hQOuXK.QJ8.jCp1/M8zCzJ2LqZq5hQOuXK.QJ', 'jane.manager@company.com', 'Jane', 'Brown', true, NOW(), NOW(), '{"roles": ["investigation_manager", "manager", "user"], "department": "investigation", "queue": "findings-assessment-queue"}'),
(6, 'tom.hr', '$2a$10$K8S0QlQYJ1YqZc5zq5hQOuXK.QJ8.jCp1/M8zCzJ2LqZq5hQOuXK.QJ', 'tom.hr@company.com', 'Tom', 'Anderson', true, NOW(), NOW(), '{"roles": ["hr_specialist", "user"], "department": "hr", "queue": "hr-discipline-queue"}')
ON CONFLICT (username) DO NOTHING;

-- Insert departments if they don't exist
INSERT INTO departments (id, name, code, description, active, created_at, updated_at) VALUES
(1, 'Intake Department', 'INTAKE', 'Initial case intake and triage', true, NOW(), NOW()),
(2, 'Investigation Department', 'INVESTIGATION', 'Case investigation and evidence gathering', true, NOW(), NOW()),
(3, 'Legal Department', 'LEGAL', 'Legal counsel and compliance', true, NOW(), NOW()),
(4, 'HR Department', 'HR', 'Human resources and employee relations', true, NOW(), NOW()),
(5, 'Administration', 'ADMIN', 'System administration and oversight', true, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

-- Insert business applications
INSERT INTO business_applications (id, name, code, description, active, created_at, updated_at) VALUES
(1, 'OneCMS System', 'onecms', 'Case Management System for investigation workflows', true, NOW(), NOW()),
(2, 'Workflow Engine', 'workflow', 'Flowable BPMN workflow engine', true, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

-- Insert roles
INSERT INTO roles (id, name, code, description, permissions, active, created_at, updated_at) VALUES
(1, 'Intake Analyst', 'intake_analyst', 'Performs initial case intake and triage', '["case:create", "case:read", "case:update"]', true, NOW(), NOW()),
(2, 'Investigator', 'investigator', 'Conducts case investigations', '["case:read", "case:update", "case:investigate"]', true, NOW(), NOW()),
(3, 'Legal Counsel', 'legal_counsel', 'Provides legal guidance and review', '["case:read", "case:review", "case:legal_advice"]', true, NOW(), NOW()),
(4, 'System Administrator', 'admin', 'Full system administration access', '["*:*"]', true, NOW(), NOW()),
(5, 'Investigation Manager', 'investigation_manager', 'Manages investigation processes', '["case:read", "case:update", "case:assign", "case:approve"]', true, NOW(), NOW()),
(6, 'HR Specialist', 'hr_specialist', 'Human resources case handling', '["case:read", "case:update", "case:hr_action"]', true, NOW(), NOW()),
(7, 'User', 'user', 'Basic user permissions', '["case:read"]', true, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

-- Insert user-role assignments using the user_business_app_roles table
INSERT INTO user_business_app_roles (id, user_id, business_app_id, role_id, assigned_at, assigned_by, active) VALUES
-- Alice (Intake Analyst)
(1, 1, 1, 1, NOW(), 4, true), -- intake_analyst role in onecms
(2, 1, 1, 7, NOW(), 4, true), -- user role in onecms

-- Edward (Investigator)
(3, 2, 1, 2, NOW(), 4, true), -- investigator role in onecms
(4, 2, 1, 7, NOW(), 4, true), -- user role in onecms

-- Sarah (Legal Counsel)
(5, 3, 1, 3, NOW(), 4, true), -- legal_counsel role in onecms
(6, 3, 1, 7, NOW(), 4, true), -- user role in onecms

-- Mike (Admin)
(7, 4, 1, 4, NOW(), 4, true), -- admin role in onecms
(8, 4, 2, 4, NOW(), 4, true), -- admin role in workflow
(9, 4, 1, 7, NOW(), 4, true), -- user role in onecms

-- Jane (Investigation Manager)
(10, 5, 1, 5, NOW(), 4, true), -- investigation_manager role in onecms
(11, 5, 1, 7, NOW(), 4, true), -- user role in onecms

-- Tom (HR Specialist)
(12, 6, 1, 6, NOW(), 4, true), -- hr_specialist role in onecms
(13, 6, 1, 7, NOW(), 4, true)  -- user role in onecms
ON CONFLICT (id) DO NOTHING;

-- Create sessions table if it doesn't exist
CREATE TABLE IF NOT EXISTS user_sessions (
    id VARCHAR(255) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    last_accessed TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    active BOOLEAN DEFAULT true
);

-- Insert some test sessions for immediate testing
INSERT INTO user_sessions (id, user_id, created_at, expires_at, last_accessed, active) VALUES
('test-session-alice', 1, NOW(), NOW() + INTERVAL '24 HOURS', NOW(), true),
('test-session-edward', 2, NOW(), NOW() + INTERVAL '24 HOURS', NOW(), true),
('test-session-sarah', 3, NOW(), NOW() + INTERVAL '24 HOURS', NOW(), true),
('test-session-mike', 4, NOW(), NOW() + INTERVAL '24 HOURS', NOW(), true),
('test-session-jane', 5, NOW(), NOW() + INTERVAL '24 HOURS', NOW(), true),
('test-session-tom', 6, NOW(), NOW() + INTERVAL '24 HOURS', NOW(), true)
ON CONFLICT (id) DO NOTHING;

-- Insert some test case data to demonstrate functionality
SET search_path TO onecms;

INSERT INTO case_types (id, name, code, description, active, created_at, updated_at) VALUES
(1, 'Employee Misconduct', 'EMPLOYEE_MISCONDUCT', 'Cases involving employee behavioral issues', true, NOW(), NOW()),
(2, 'Harassment Investigation', 'HARASSMENT', 'Workplace harassment investigations', true, NOW(), NOW()),
(3, 'Fraud Investigation', 'FRAUD', 'Financial fraud and misuse investigations', true, NOW(), NOW()),
(4, 'Policy Violation', 'POLICY_VIOLATION', 'Corporate policy violation cases', true, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

-- Verify data insertion
SELECT 'Users created:' as info, COUNT(*) as count FROM entitlements.users;
SELECT 'Departments created:' as info, COUNT(*) as count FROM entitlements.departments;
SELECT 'Roles created:' as info, COUNT(*) as count FROM entitlements.roles;
SELECT 'User role assignments:' as info, COUNT(*) as count FROM entitlements.user_business_app_roles;
SELECT 'Active sessions:' as info, COUNT(*) as count FROM entitlements.user_sessions;
SELECT 'Case types created:' as info, COUNT(*) as count FROM onecms.case_types;

-- Display created test users
SELECT u.username, u.email, u.active, u.attributes->'roles' as roles 
FROM entitlements.users u 
WHERE u.username IN ('alice.intake', 'edward.inv', 'sarah.legal', 'mike.admin', 'jane.manager', 'tom.hr');

COMMIT;