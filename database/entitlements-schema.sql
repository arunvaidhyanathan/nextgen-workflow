-- Entitlements Schema for NextGen Workflow
-- This schema manages users, roles, departments, queues, and permissions
-- Using sequences instead of UUIDs, with proper entity naming conventions

SET search_path TO entitlements;

-- =====================================================
-- SEQUENCES
-- =====================================================

CREATE SEQUENCE IF NOT EXISTS user_id_seq START WITH 1000;
CREATE SEQUENCE IF NOT EXISTS department_id_seq START WITH 100;
CREATE SEQUENCE IF NOT EXISTS role_id_seq START WITH 100;
CREATE SEQUENCE IF NOT EXISTS queue_id_seq START WITH 100;
CREATE SEQUENCE IF NOT EXISTS permission_id_seq START WITH 1000;
CREATE SEQUENCE IF NOT EXISTS user_role_id_seq START WITH 10000;
CREATE SEQUENCE IF NOT EXISTS user_department_id_seq START WITH 10000;
CREATE SEQUENCE IF NOT EXISTS user_queue_access_id_seq START WITH 10000;
CREATE SEQUENCE IF NOT EXISTS role_permission_id_seq START WITH 10000;
CREATE SEQUENCE IF NOT EXISTS workflow_permission_id_seq START WITH 10000;
CREATE SEQUENCE IF NOT EXISTS session_id_seq START WITH 100000;

-- =====================================================
-- REFERENCE TABLES
-- =====================================================

-- Departments table
CREATE TABLE IF NOT EXISTS departments (
    department_id BIGINT PRIMARY KEY DEFAULT nextval('department_id_seq'),
    department_code VARCHAR(50) UNIQUE NOT NULL,
    department_name VARCHAR(255) NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Queues table (work queues)
CREATE TABLE IF NOT EXISTS queues (
    queue_id BIGINT PRIMARY KEY DEFAULT nextval('queue_id_seq'),
    queue_name VARCHAR(255) UNIQUE NOT NULL,
    queue_display_name VARCHAR(255) NOT NULL,
    description TEXT,
    queue_type VARCHAR(50) NOT NULL, -- 'WORK_ITEMS', 'OPEN_CASES', 'INVESTIGATIONS'
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Workflow phases
CREATE TABLE IF NOT EXISTS workflow_phases (
    phase_name VARCHAR(100) PRIMARY KEY,
    phase_display_name VARCHAR(255) NOT NULL,
    phase_order INTEGER NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Workflow steps/actions
CREATE TABLE IF NOT EXISTS workflow_steps (
    step_name VARCHAR(255) PRIMARY KEY,
    step_display_name VARCHAR(255) NOT NULL,
    phase_name VARCHAR(100) REFERENCES workflow_phases(phase_name),
    description TEXT,
    step_order INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- CORE TABLES
-- =====================================================

-- Users table
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT PRIMARY KEY DEFAULT nextval('user_id_seq'),
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255), -- For session-based auth
    is_active BOOLEAN DEFAULT TRUE,
    is_system_admin BOOLEAN DEFAULT FALSE,
    last_login_at TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(user_id),
    updated_by BIGINT REFERENCES users(user_id),
    attributes JSONB DEFAULT '{}' -- Additional user attributes for ABAC
);

-- Roles table
CREATE TABLE IF NOT EXISTS roles (
    role_id BIGINT PRIMARY KEY DEFAULT nextval('role_id_seq'),
    role_code VARCHAR(100) UNIQUE NOT NULL,
    role_name VARCHAR(255) NOT NULL,
    description TEXT,
    role_type VARCHAR(50) NOT NULL, -- 'SYSTEM', 'DEPARTMENT', 'WORKFLOW'
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB DEFAULT '{}'
);

-- Permissions table (for RBAC)
CREATE TABLE IF NOT EXISTS permissions (
    permission_id BIGINT PRIMARY KEY DEFAULT nextval('permission_id_seq'),
    permission_code VARCHAR(255) UNIQUE NOT NULL,
    permission_name VARCHAR(255) NOT NULL,
    resource_type VARCHAR(100) NOT NULL, -- 'CASE', 'TASK', 'USER', 'WORKFLOW'
    action VARCHAR(100) NOT NULL, -- 'CREATE', 'READ', 'UPDATE', 'DELETE', 'EXECUTE'
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- RELATIONSHIP TABLES
-- =====================================================

-- User-Department assignments
CREATE TABLE IF NOT EXISTS user_departments (
    user_department_id BIGINT PRIMARY KEY DEFAULT nextval('user_department_id_seq'),
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    department_id BIGINT NOT NULL REFERENCES departments(department_id),
    is_primary BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT REFERENCES users(user_id),
    expires_at TIMESTAMP,
    UNIQUE(user_id, department_id)
);

-- User-Role assignments
CREATE TABLE IF NOT EXISTS user_roles (
    user_role_id BIGINT PRIMARY KEY DEFAULT nextval('user_role_id_seq'),
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    role_id BIGINT NOT NULL REFERENCES roles(role_id),
    department_id BIGINT REFERENCES departments(department_id), -- Role can be department-specific
    is_active BOOLEAN DEFAULT TRUE,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    granted_by BIGINT REFERENCES users(user_id),
    expires_at TIMESTAMP,
    metadata JSONB DEFAULT '{}',
    UNIQUE(user_id, role_id, department_id)
);

-- User-Queue access
CREATE TABLE IF NOT EXISTS user_queue_access (
    user_queue_access_id BIGINT PRIMARY KEY DEFAULT nextval('user_queue_access_id_seq'),
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    queue_id BIGINT NOT NULL REFERENCES queues(queue_id),
    access_type VARCHAR(50) NOT NULL, -- 'VIEW', 'CLAIM', 'COMPLETE'
    is_active BOOLEAN DEFAULT TRUE,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    granted_by BIGINT REFERENCES users(user_id),
    UNIQUE(user_id, queue_id, access_type)
);

-- Role-Permission mappings
CREATE TABLE IF NOT EXISTS role_permissions (
    role_permission_id BIGINT PRIMARY KEY DEFAULT nextval('role_permission_id_seq'),
    role_id BIGINT NOT NULL REFERENCES roles(role_id),
    permission_id BIGINT NOT NULL REFERENCES permissions(permission_id),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, permission_id)
);

-- Workflow step permissions (who can perform which workflow steps)
CREATE TABLE IF NOT EXISTS workflow_permissions (
    workflow_permission_id BIGINT PRIMARY KEY DEFAULT nextval('workflow_permission_id_seq'),
    role_id BIGINT NOT NULL REFERENCES roles(role_id),
    workflow_step VARCHAR(255) NOT NULL REFERENCES workflow_steps(step_name),
    department_id BIGINT REFERENCES departments(department_id), -- Can be department-specific
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, workflow_step, department_id)
);

-- =====================================================
-- SESSION MANAGEMENT
-- =====================================================

-- User sessions table
CREATE TABLE IF NOT EXISTS user_sessions (
    session_id VARCHAR(255) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    metadata JSONB DEFAULT '{}'
);

-- =====================================================
-- AUDIT TABLES
-- =====================================================

-- Audit log for tracking changes
CREATE TABLE IF NOT EXISTS audit_log (
    audit_id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL,
    record_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL, -- 'INSERT', 'UPDATE', 'DELETE'
    changed_by BIGINT REFERENCES users(user_id),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT
);

-- =====================================================
-- INDEXES
-- =====================================================

CREATE INDEX idx_users_username ON users(username) WHERE is_active = TRUE;
CREATE INDEX idx_users_email ON users(email) WHERE is_active = TRUE;
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id) WHERE is_active = TRUE;
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id) WHERE is_active = TRUE;
CREATE INDEX idx_user_departments_user_id ON user_departments(user_id) WHERE is_active = TRUE;
CREATE INDEX idx_user_queue_access_user_id ON user_queue_access(user_id) WHERE is_active = TRUE;
CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id) WHERE is_active = TRUE;
CREATE INDEX idx_user_sessions_expires_at ON user_sessions(expires_at) WHERE is_active = TRUE;
CREATE INDEX idx_audit_log_table_record ON audit_log(table_name, record_id);
CREATE INDEX idx_audit_log_changed_by ON audit_log(changed_by);

-- =====================================================
-- INITIAL DATA
-- =====================================================

-- Insert departments
INSERT INTO departments (department_code, department_name, description) VALUES
('EO', 'Ethics Office', 'Handles ethics-related cases and compliance'),
('ER', 'Employee Relations', 'Manages employee relations and workplace issues'),
('CSIS', 'Corporate Security & Information Security', 'Handles security and information security cases')
ON CONFLICT (department_code) DO NOTHING;

-- Insert workflow phases
INSERT INTO workflow_phases (phase_name, phase_display_name, phase_order) VALUES
('INTAKE', 'Intake', 1),
('INVESTIGATION', 'Investigation', 2),
('CLOSURE', 'Closure', 3)
ON CONFLICT (phase_name) DO NOTHING;

-- Insert workflow steps
INSERT INTO workflow_steps (step_name, step_display_name, phase_name, step_order) VALUES
('CREATE_CASE', 'Create Case', 'INTAKE', 1),
('REJECT_CASE', 'Reject Case', 'INTAKE', 2),
('ASSIGN_TO_EO_OFFICER', 'Assign to EO Officer', 'INTAKE', 3),
('SEND_BACK_TO_EO_INTAKE', 'Send back to EO Intake', 'INTAKE', 4),
('ASSIGN_TO_IU', 'Assign to IU', 'INTAKE', 5),
('CLOSURE_REVIEW', 'Closure Review (Valid Investigation sent back by IU)', 'INTAKE', 6),
('APPROVE_CLOSURES', 'Approve closures', 'CLOSURE', 1),
('SEND_BACK_TO_EO', 'Send Back (to EO)', 'INTAKE', 7),
('ASSIGN_TO_HRPS', 'Assign to HRPS', 'INTAKE', 8),
('ASSIGN_TO_INVESTIGATION_MANAGER', 'Assign to Investigation Manager', 'INTAKE', 9),
('SEND_BACK', 'Send Back', 'INVESTIGATION', 1),
('ASSIGN_TO_INVESTIGATOR', 'Assign to Investigator', 'INVESTIGATION', 2),
('IP_REVIEW', 'IP Review', 'INVESTIGATION', 3),
('INVESTIGATION_REPORT', 'Investigation Report', 'INVESTIGATION', 4),
('SEND_TO_DISCIPLINE', 'Send for Discipline', 'INVESTIGATION', 5),
('CASE_CLOSURE', 'Case Closure', 'CLOSURE', 2),
('CLOSE_CASE_AND_RETAIN_FOR_INTELLIGENCE', 'Close Case and Retain for intelligence', 'CLOSURE', 3)
ON CONFLICT (step_name) DO NOTHING;

-- Insert queues
INSERT INTO queues (queue_name, queue_display_name, queue_type) VALUES
('my-work-items', 'My Work Items', 'WORK_ITEMS'),
('all-open-cases', 'All Open Cases', 'OPEN_CASES'),
('my-open-investigations', 'My Open Investigations', 'INVESTIGATIONS'),
('intake-analyst-queue', 'Intake Analyst Queue', 'WORK_ITEMS'),
('eo-officer-queue', 'EO Officer Queue', 'WORK_ITEMS'),
('eo-head-queue', 'EO Head Queue', 'WORK_ITEMS'),
('investigation-manager-queue', 'Investigation Manager Queue', 'WORK_ITEMS'),
('investigator-queue', 'Investigator Queue', 'WORK_ITEMS'),
('csis-intake-manager-queue', 'CSIS Intake Manager Queue', 'WORK_ITEMS')
ON CONFLICT (queue_name) DO NOTHING;

-- Insert roles
INSERT INTO roles (role_code, role_name, role_type) VALUES
-- EO Roles
('EO_INTAKE_ANALYST', 'EO Intake Analyst', 'DEPARTMENT'),
('EO_OFFICER', 'EO Officer', 'DEPARTMENT'),
('EO_HEAD', 'EO Head', 'DEPARTMENT'),
-- ER Roles
('ER_INTAKE_ANALYST', 'ER Intake Analyst', 'DEPARTMENT'),
('ER_INVESTIGATION_MANAGER', 'ER Investigation Manager', 'DEPARTMENT'),
('ER_INVESTIGATOR', 'ER Investigator', 'DEPARTMENT'),
-- CSIS Roles
('CSIS_INTAKE_ANALYST', 'CSIS Intake Analyst', 'DEPARTMENT'),
('CSIS_INTAKE_MANAGER', 'CSIS Intake Manager', 'DEPARTMENT'),
('CSIS_INVESTIGATION_MANAGER', 'CSIS Investigation Manager', 'DEPARTMENT'),
('CSIS_INVESTIGATOR', 'CSIS Investigator', 'DEPARTMENT'),
-- System Roles
('SYSTEM_ADMIN', 'System Administrator', 'SYSTEM'),
('WORKFLOW_ADMIN', 'Workflow Administrator', 'SYSTEM')
ON CONFLICT (role_code) DO NOTHING;

-- Insert base permissions
INSERT INTO permissions (permission_code, permission_name, resource_type, action) VALUES
-- Case permissions
('CASE_CREATE', 'Create Case', 'CASE', 'CREATE'),
('CASE_READ', 'Read Case', 'CASE', 'READ'),
('CASE_UPDATE', 'Update Case', 'CASE', 'UPDATE'),
('CASE_DELETE', 'Delete Case', 'CASE', 'DELETE'),
('CASE_ASSIGN', 'Assign Case', 'CASE', 'EXECUTE'),
('CASE_REJECT', 'Reject Case', 'CASE', 'EXECUTE'),
('CASE_APPROVE', 'Approve Case', 'CASE', 'EXECUTE'),
-- Task permissions
('TASK_CLAIM', 'Claim Task', 'TASK', 'EXECUTE'),
('TASK_COMPLETE', 'Complete Task', 'TASK', 'EXECUTE'),
('TASK_REASSIGN', 'Reassign Task', 'TASK', 'EXECUTE'),
('TASK_VIEW', 'View Task', 'TASK', 'READ'),
-- User permissions
('USER_CREATE', 'Create User', 'USER', 'CREATE'),
('USER_READ', 'Read User', 'USER', 'READ'),
('USER_UPDATE', 'Update User', 'USER', 'UPDATE'),
('USER_DELETE', 'Delete User', 'USER', 'DELETE'),
-- Workflow permissions
('WORKFLOW_START', 'Start Workflow', 'WORKFLOW', 'EXECUTE'),
('WORKFLOW_CANCEL', 'Cancel Workflow', 'WORKFLOW', 'EXECUTE'),
('WORKFLOW_VIEW', 'View Workflow', 'WORKFLOW', 'READ')
ON CONFLICT (permission_code) DO NOTHING;

-- =====================================================
-- FUNCTIONS
-- =====================================================

-- Function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_departments_updated_at BEFORE UPDATE ON departments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_roles_updated_at BEFORE UPDATE ON roles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_queues_updated_at BEFORE UPDATE ON queues
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Function to clean up expired sessions
CREATE OR REPLACE FUNCTION cleanup_expired_sessions()
RETURNS void AS $$
BEGIN
    UPDATE user_sessions 
    SET is_active = FALSE 
    WHERE expires_at < CURRENT_TIMESTAMP AND is_active = TRUE;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- COMMENTS
-- =====================================================

COMMENT ON SCHEMA entitlements IS 'Schema for user management, roles, and authorization';
COMMENT ON TABLE users IS 'Core user accounts table with proper naming conventions';
COMMENT ON TABLE departments IS 'Organization departments (EO, ER, CSIS)';
COMMENT ON TABLE roles IS 'System and department-specific roles';
COMMENT ON TABLE permissions IS 'RBAC permissions for resources and actions';
COMMENT ON TABLE user_sessions IS 'Session-based authentication tracking';
COMMENT ON TABLE workflow_permissions IS 'Maps roles to allowed workflow steps';
COMMENT ON TABLE audit_log IS 'Comprehensive audit trail for all changes';