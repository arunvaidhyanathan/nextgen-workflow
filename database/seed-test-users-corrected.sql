-- Corrected Seed Test Users for NextGen Workflow System
-- Based on actual database schema structure

\c nextgen_workflow;

-- Insert test users (password will be handled by the application)
INSERT INTO entitlements.users (id, username, email, first_name, last_name, is_active, attributes) VALUES
('user-001', 'alice.intake', 'alice.intake@company.com', 'Alice', 'Johnson', true, '{"roles": ["intake_analyst", "user"], "department": "intake", "queue": "intake-analyst-queue", "password_hash": "$2a$10$K8S0QlQYJ1YqZc5zq5hQOuXK.QJ8.jCp1/M8zCzJ2LqZq5hQOuXK.QJ"}'),
('user-002', 'edward.inv', 'edward.inv@company.com', 'Edward', 'Smith', true, '{"roles": ["investigator", "user"], "department": "investigation", "queue": "investigation-queue", "password_hash": "$2a$10$K8S0QlQYJ1YqZc5zq5hQOuXK.QJ8.jCp1/M8zCzJ2LqZq5hQOuXK.QJ"}'),
('user-003', 'sarah.legal', 'sarah.legal@company.com', 'Sarah', 'Wilson', true, '{"roles": ["legal_counsel", "hr_legal", "user"], "department": "legal", "queue": "hr-legal-queue", "password_hash": "$2a$10$K8S0QlQYJ1YqZc5zq5hQOuXK.QJ8.jCp1/M8zCzJ2LqZq5hQOuXK.QJ"}'),
('user-004', 'mike.admin', 'mike.admin@company.com', 'Mike', 'Davis', true, '{"roles": ["admin", "system_admin", "user"], "department": "admin", "queue": "admin-queue", "password_hash": "$2a$10$K8S0QlQYJ1YqZc5zq5hQOuXK.QJ8.jCp1/M8zCzJ2LqZq5hQOuXK.QJ"}'),
('user-005', 'jane.manager', 'jane.manager@company.com', 'Jane', 'Brown', true, '{"roles": ["investigation_manager", "manager", "user"], "department": "investigation", "queue": "findings-assessment-queue", "password_hash": "$2a$10$K8S0QlQYJ1YqZc5zq5hQOuXK.QJ8.jCp1/M8zCzJ2LqZq5hQOuXK.QJ"}'),
('user-006', 'tom.hr', 'tom.hr@company.com', 'Tom', 'Anderson', true, '{"roles": ["hr_specialist", "user"], "department": "hr", "queue": "hr-discipline-queue", "password_hash": "$2a$10$K8S0QlQYJ1YqZc5zq5hQOuXK.QJ8.jCp1/M8zCzJ2LqZq5hQOuXK.QJ"}')
ON CONFLICT (id) DO NOTHING;

-- Insert departments
INSERT INTO entitlements.departments (department_name, department_code, description, is_active) VALUES
('Intake Department', 'INTAKE', 'Initial case intake and triage', true),
('Investigation Department', 'INVESTIGATION', 'Case investigation and evidence gathering', true),
('Legal Department', 'LEGAL', 'Legal counsel and compliance', true),
('HR Department', 'HR', 'Human resources and employee relations', true),
('Administration', 'ADMIN', 'System administration and oversight', true)
ON CONFLICT (department_code) DO NOTHING;

-- Insert business applications
INSERT INTO entitlements.business_applications (business_app_name, description, is_active, metadata) VALUES
('onecms', 'Case Management System for investigation workflows', true, '{"version": "1.0", "type": "case_management"}'),
('workflow', 'Flowable BPMN workflow engine', true, '{"version": "1.0", "type": "workflow_engine"}')
ON CONFLICT (business_app_name) DO NOTHING;

-- Create user sessions table for session-based authentication
CREATE TABLE IF NOT EXISTS entitlements.user_sessions (
    session_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL REFERENCES entitlements.users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_accessed TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    ip_address VARCHAR(45),
    user_agent TEXT,
    is_active BOOLEAN DEFAULT true
);

-- Insert test sessions for immediate API testing
INSERT INTO entitlements.user_sessions (session_id, user_id, expires_at, is_active) VALUES
('test-session-alice', 'user-001', NOW() + INTERVAL '24 HOURS', true),
('test-session-edward', 'user-002', NOW() + INTERVAL '24 HOURS', true),
('test-session-sarah', 'user-003', NOW() + INTERVAL '24 HOURS', true),
('test-session-mike', 'user-004', NOW() + INTERVAL '24 HOURS', true),
('test-session-jane', 'user-005', NOW() + INTERVAL '24 HOURS', true),
('test-session-tom', 'user-006', NOW() + INTERVAL '24 HOURS', true)
ON CONFLICT (session_id) DO NOTHING;

-- Link users to departments
INSERT INTO entitlements.user_departments (user_id, department_id) 
SELECT u.id, d.id 
FROM entitlements.users u, entitlements.departments d 
WHERE (u.username = 'alice.intake' AND d.department_code = 'INTAKE')
   OR (u.username = 'edward.inv' AND d.department_code = 'INVESTIGATION')
   OR (u.username = 'sarah.legal' AND d.department_code = 'LEGAL')
   OR (u.username = 'mike.admin' AND d.department_code = 'ADMIN')
   OR (u.username = 'jane.manager' AND d.department_code = 'INVESTIGATION')
   OR (u.username = 'tom.hr' AND d.department_code = 'HR')
ON CONFLICT DO NOTHING;

-- Link users to business applications with roles (if the table supports it)
INSERT INTO entitlements.user_business_app_roles (user_id, business_app_id, assigned_at, is_active) 
SELECT u.id, ba.id, NOW(), true
FROM entitlements.users u, entitlements.business_applications ba 
WHERE ba.business_app_name IN ('onecms', 'workflow')
ON CONFLICT DO NOTHING;

-- Insert some sample case types in the onecms schema
INSERT INTO onecms.case_types (type_name, type_code, description, is_active) VALUES
('Employee Misconduct', 'EMPLOYEE_MISCONDUCT', 'Cases involving employee behavioral issues', true),
('Harassment Investigation', 'HARASSMENT', 'Workplace harassment investigations', true),
('Fraud Investigation', 'FRAUD', 'Financial fraud and misuse investigations', true),
('Policy Violation', 'POLICY_VIOLATION', 'Corporate policy violation cases', true)
ON CONFLICT (type_code) DO NOTHING;

-- Verify data insertion
SELECT 'Users created:' as info, COUNT(*) as count FROM entitlements.users;
SELECT 'Departments created:' as info, COUNT(*) as count FROM entitlements.departments;
SELECT 'Business applications:' as info, COUNT(*) as count FROM entitlements.business_applications;
SELECT 'Active sessions:' as info, COUNT(*) as count FROM entitlements.user_sessions;

-- Display created test users with their details
SELECT 
    u.username, 
    u.email, 
    u.is_active, 
    u.attributes->'roles' as roles,
    u.attributes->'department' as department,
    u.attributes->'queue' as queue
FROM entitlements.users u 
WHERE u.username IN ('alice.intake', 'edward.inv', 'sarah.legal', 'mike.admin', 'jane.manager', 'tom.hr')
ORDER BY u.username;

-- Display active sessions
SELECT session_id, user_id, expires_at FROM entitlements.user_sessions WHERE is_active = true;

COMMIT;