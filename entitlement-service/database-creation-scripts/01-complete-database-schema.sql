-- =====================================================================
-- NextGen Workflow Entitlement Service - Complete Database Schema
-- Version: 2.0.0
-- Created: 2025-08-28
-- Description: Complete table creation script for entitlement service
--              including core RBAC and hybrid RBAC/ABAC authorization
-- =====================================================================

-- Create schema
CREATE SCHEMA IF NOT EXISTS entitlements;

-- Set search path
SET search_path TO entitlements, public;

-- =====================================================================
-- PART 1: CORE ENTITLEMENT TABLES (Simple RBAC)
-- =====================================================================

-- ---------------------------------------------------------------------
-- Users Table (Primary identity store)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS entitlements.users (
    id VARCHAR(50) PRIMARY KEY,                                     -- Manual string-based user ID
    username VARCHAR(100) NOT NULL UNIQUE,                          -- Login username
    email VARCHAR(255) NOT NULL UNIQUE,                            -- Email address (unique)
    first_name VARCHAR(100) NOT NULL,                              -- First name
    last_name VARCHAR(100) NOT NULL,                               -- Last name
    is_active BOOLEAN NOT NULL DEFAULT true,                       -- Active status flag
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,     -- Creation timestamp
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,     -- Last update timestamp
    attributes JSONB DEFAULT '{}',                                 -- Flexible user attributes (JSON)
    
    -- Constraints
    CONSTRAINT chk_users_id_length CHECK (char_length(id) >= 1 AND char_length(id) <= 50),
    CONSTRAINT chk_users_username_length CHECK (char_length(username) >= 3 AND char_length(username) <= 100),
    CONSTRAINT chk_users_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

-- Index for user lookups
CREATE INDEX IF NOT EXISTS idx_users_username ON entitlements.users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON entitlements.users(email);
CREATE INDEX IF NOT EXISTS idx_users_active ON entitlements.users(is_active);
CREATE INDEX IF NOT EXISTS idx_users_created ON entitlements.users(created_at);
CREATE INDEX IF NOT EXISTS idx_users_attributes_gin ON entitlements.users USING gin(attributes);

-- Update trigger for updated_at
CREATE OR REPLACE FUNCTION entitlements.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER tr_users_updated_at 
    BEFORE UPDATE ON entitlements.users 
    FOR EACH ROW EXECUTE FUNCTION entitlements.update_updated_at_column();

-- ---------------------------------------------------------------------
-- Departments Table (Organizational units)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS entitlements.departments (
    id BIGSERIAL PRIMARY KEY,                                      -- Auto-increment primary key
    department_name VARCHAR(100) NOT NULL UNIQUE,                  -- Department name (unique)
    department_code VARCHAR(50) NOT NULL UNIQUE,                   -- Department code (unique)
    description TEXT,                                               -- Department description
    is_active BOOLEAN NOT NULL DEFAULT true,                       -- Active status flag
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,     -- Creation timestamp
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,     -- Last update timestamp
    
    -- Constraints
    CONSTRAINT chk_departments_name_length CHECK (char_length(department_name) >= 2),
    CONSTRAINT chk_departments_code_length CHECK (char_length(department_code) >= 2),
    CONSTRAINT chk_departments_code_format CHECK (department_code ~ '^[A-Z0-9_-]+$')
);

-- Indexes for department lookups
CREATE INDEX IF NOT EXISTS idx_departments_name ON entitlements.departments(department_name);
CREATE INDEX IF NOT EXISTS idx_departments_code ON entitlements.departments(department_code);
CREATE INDEX IF NOT EXISTS idx_departments_active ON entitlements.departments(is_active);

-- Update trigger for departments
CREATE TRIGGER tr_departments_updated_at 
    BEFORE UPDATE ON entitlements.departments 
    FOR EACH ROW EXECUTE FUNCTION entitlements.update_updated_at_column();

-- ---------------------------------------------------------------------
-- Business Applications Table (Systems/Applications)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS entitlements.business_applications (
    id BIGSERIAL PRIMARY KEY,                                      -- Auto-increment primary key
    business_app_name VARCHAR(100) NOT NULL UNIQUE,                -- Application name (unique)
    description TEXT,                                               -- Application description
    is_active BOOLEAN NOT NULL DEFAULT true,                       -- Active status flag
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,     -- Creation timestamp
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,     -- Last update timestamp
    metadata JSONB DEFAULT '{}',                                   -- Application metadata (JSON)
    
    -- Constraints
    CONSTRAINT chk_business_apps_name_length CHECK (char_length(business_app_name) >= 2),
    CONSTRAINT chk_business_apps_name_format CHECK (business_app_name ~ '^[a-z0-9_-]+$')
);

-- Indexes for business application lookups
CREATE INDEX IF NOT EXISTS idx_business_apps_name ON entitlements.business_applications(business_app_name);
CREATE INDEX IF NOT EXISTS idx_business_apps_active ON entitlements.business_applications(is_active);
CREATE INDEX IF NOT EXISTS idx_business_apps_metadata_gin ON entitlements.business_applications USING gin(metadata);

-- Update trigger for business applications
CREATE TRIGGER tr_business_apps_updated_at 
    BEFORE UPDATE ON entitlements.business_applications 
    FOR EACH ROW EXECUTE FUNCTION entitlements.update_updated_at_column();

-- ---------------------------------------------------------------------
-- Business App Roles Table (Application-specific roles)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS entitlements.business_app_roles (
    id BIGSERIAL PRIMARY KEY,                                      -- Auto-increment primary key
    business_app_id BIGINT NOT NULL,                               -- Reference to business application
    role_name VARCHAR(100) NOT NULL,                               -- Role name within application
    role_display_name VARCHAR(255) NOT NULL,                       -- Human-readable role name
    description TEXT,                                               -- Role description
    is_active BOOLEAN NOT NULL DEFAULT true,                       -- Active status flag
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,     -- Creation timestamp
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,     -- Last update timestamp
    metadata JSONB DEFAULT '{}',                                   -- Role metadata (permissions, queues, etc.)
    
    -- Foreign key constraint
    CONSTRAINT fk_business_app_roles_app FOREIGN KEY (business_app_id) 
        REFERENCES entitlements.business_applications(id) ON DELETE CASCADE,
    
    -- Unique constraint (app + role name)
    CONSTRAINT uk_business_app_role UNIQUE(business_app_id, role_name),
    
    -- Check constraints
    CONSTRAINT chk_business_app_roles_name_length CHECK (char_length(role_name) >= 2),
    CONSTRAINT chk_business_app_roles_name_format CHECK (role_name ~ '^[A-Z0-9_]+$')
);

-- Indexes for business app roles
CREATE INDEX IF NOT EXISTS idx_business_app_roles_app ON entitlements.business_app_roles(business_app_id);
CREATE INDEX IF NOT EXISTS idx_business_app_roles_name ON entitlements.business_app_roles(role_name);
CREATE INDEX IF NOT EXISTS idx_business_app_roles_active ON entitlements.business_app_roles(is_active);
CREATE INDEX IF NOT EXISTS idx_business_app_roles_metadata_gin ON entitlements.business_app_roles USING gin(metadata);

-- Update trigger for business app roles
CREATE TRIGGER tr_business_app_roles_updated_at 
    BEFORE UPDATE ON entitlements.business_app_roles 
    FOR EACH ROW EXECUTE FUNCTION entitlements.update_updated_at_column();

-- ---------------------------------------------------------------------
-- User Departments Table (Many-to-many: Users ↔ Departments)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS entitlements.user_departments (
    id BIGSERIAL PRIMARY KEY,                                      -- Auto-increment primary key
    user_id VARCHAR(50) NOT NULL,                                  -- Reference to user
    department_id BIGINT NOT NULL,                                 -- Reference to department
    is_active BOOLEAN NOT NULL DEFAULT true,                       -- Active assignment flag
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,     -- Assignment timestamp
    
    -- Foreign key constraints
    CONSTRAINT fk_user_departments_user FOREIGN KEY (user_id) 
        REFERENCES entitlements.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_departments_department FOREIGN KEY (department_id) 
        REFERENCES entitlements.departments(id) ON DELETE CASCADE,
    
    -- Unique constraint (prevent duplicate assignments)
    CONSTRAINT uk_user_department UNIQUE(user_id, department_id)
);

-- Indexes for user departments
CREATE INDEX IF NOT EXISTS idx_user_departments_user ON entitlements.user_departments(user_id);
CREATE INDEX IF NOT EXISTS idx_user_departments_department ON entitlements.user_departments(department_id);
CREATE INDEX IF NOT EXISTS idx_user_departments_active ON entitlements.user_departments(is_active);
CREATE INDEX IF NOT EXISTS idx_user_departments_created ON entitlements.user_departments(created_at);

-- ---------------------------------------------------------------------
-- User Business App Roles Table (Many-to-many: Users ↔ Roles)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS entitlements.user_business_app_roles (
    id BIGSERIAL PRIMARY KEY,                                      -- Auto-increment primary key
    user_id VARCHAR(50) NOT NULL,                                  -- Reference to user
    business_app_role_id BIGINT NOT NULL,                          -- Reference to business app role
    is_active BOOLEAN NOT NULL DEFAULT true,                       -- Active assignment flag
    
    -- Foreign key constraints
    CONSTRAINT fk_user_business_app_roles_user FOREIGN KEY (user_id) 
        REFERENCES entitlements.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_business_app_roles_role FOREIGN KEY (business_app_role_id) 
        REFERENCES entitlements.business_app_roles(id) ON DELETE CASCADE,
    
    -- Unique constraint (prevent duplicate assignments)
    CONSTRAINT uk_user_business_app_role UNIQUE(user_id, business_app_role_id)
);

-- Indexes for user business app roles
CREATE INDEX IF NOT EXISTS idx_user_business_app_roles_user ON entitlements.user_business_app_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_business_app_roles_role ON entitlements.user_business_app_roles(business_app_role_id);
CREATE INDEX IF NOT EXISTS idx_user_business_app_roles_active ON entitlements.user_business_app_roles(is_active);

-- =====================================================================
-- PART 2: HYBRID AUTHORIZATION TABLES (Advanced RBAC/ABAC)
-- =====================================================================

-- ---------------------------------------------------------------------
-- Application Domains Table (Multi-tenant support)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS entitlements.entitlement_application_domains (
    domain_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    domain_name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    is_tiered BOOLEAN DEFAULT false,
    domain_metadata JSONB DEFAULT '{}',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for domain lookups
CREATE INDEX IF NOT EXISTS idx_domains_name ON entitlements.entitlement_application_domains(domain_name);
CREATE INDEX IF NOT EXISTS idx_domains_active ON entitlements.entitlement_application_domains(is_active);
CREATE INDEX IF NOT EXISTS idx_domains_metadata_gin ON entitlements.entitlement_application_domains USING gin(domain_metadata);

-- Update trigger for domains
CREATE TRIGGER tr_domains_updated_at 
    BEFORE UPDATE ON entitlements.entitlement_application_domains 
    FOR EACH ROW EXECUTE FUNCTION entitlements.update_updated_at_column();

-- ---------------------------------------------------------------------
-- Domain Roles Table (Context-specific roles)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS entitlements.entitlement_domain_roles (
    role_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    domain_id UUID NOT NULL,
    role_name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    description TEXT,
    role_level VARCHAR(50), -- Tier1, Tier2, Manager, etc.
    maker_checker_type VARCHAR(50), -- MAKER, CHECKER, NONE, BOTH
    role_metadata JSONB DEFAULT '{}',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    CONSTRAINT fk_domain_roles_domain FOREIGN KEY (domain_id) 
        REFERENCES entitlements.entitlement_application_domains(domain_id) ON DELETE CASCADE,
    
    -- Unique constraint
    CONSTRAINT uk_domain_role UNIQUE(domain_id, role_name)
);

-- Indexes for role queries
CREATE INDEX IF NOT EXISTS idx_domain_roles_domain ON entitlements.entitlement_domain_roles(domain_id);
CREATE INDEX IF NOT EXISTS idx_domain_roles_name ON entitlements.entitlement_domain_roles(role_name);
CREATE INDEX IF NOT EXISTS idx_domain_roles_active ON entitlements.entitlement_domain_roles(is_active);
CREATE INDEX IF NOT EXISTS idx_domain_roles_level ON entitlements.entitlement_domain_roles(role_level);
CREATE INDEX IF NOT EXISTS idx_domain_roles_metadata_gin ON entitlements.entitlement_domain_roles USING gin(role_metadata);

-- Update trigger for domain roles
CREATE TRIGGER tr_domain_roles_updated_at 
    BEFORE UPDATE ON entitlements.entitlement_domain_roles 
    FOR EACH ROW EXECUTE FUNCTION entitlements.update_updated_at_column();

-- ---------------------------------------------------------------------
-- Permissions Table (Granular permissions)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS entitlements.entitlement_permissions (
    permission_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_type VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    -- Unique constraint
    CONSTRAINT uk_permission UNIQUE(resource_type, action)
);

-- Indexes for permission lookups
CREATE INDEX IF NOT EXISTS idx_permissions_resource_type ON entitlements.entitlement_permissions(resource_type);
CREATE INDEX IF NOT EXISTS idx_permissions_action ON entitlements.entitlement_permissions(action);

-- Update trigger for permissions
CREATE TRIGGER tr_permissions_updated_at 
    BEFORE UPDATE ON entitlements.entitlement_permissions 
    FOR EACH ROW EXECUTE FUNCTION entitlements.update_updated_at_column();

-- ---------------------------------------------------------------------
-- Role Permissions Table (RBAC core mapping)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS entitlements.entitlement_role_permissions (
    role_permission_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) 
        REFERENCES entitlements.entitlement_domain_roles(role_id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) 
        REFERENCES entitlements.entitlement_permissions(permission_id) ON DELETE CASCADE,
    
    -- Unique constraint
    CONSTRAINT uk_role_permission UNIQUE(role_id, permission_id)
);

-- Indexes for RBAC queries
CREATE INDEX IF NOT EXISTS idx_role_permissions_role ON entitlements.entitlement_role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission ON entitlements.entitlement_role_permissions(permission_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_active ON entitlements.entitlement_role_permissions(is_active);

-- Update trigger for role permissions
CREATE TRIGGER tr_role_permissions_updated_at 
    BEFORE UPDATE ON entitlements.entitlement_role_permissions 
    FOR EACH ROW EXECUTE FUNCTION entitlements.update_updated_at_column();

-- ---------------------------------------------------------------------
-- Enhanced Users Table (Extended identity)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS entitlements.entitlement_core_users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    global_attributes JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for enhanced users
CREATE INDEX IF NOT EXISTS idx_core_users_username ON entitlements.entitlement_core_users(username);
CREATE INDEX IF NOT EXISTS idx_core_users_email ON entitlements.entitlement_core_users(email);
CREATE INDEX IF NOT EXISTS idx_core_users_active ON entitlements.entitlement_core_users(is_active);
CREATE INDEX IF NOT EXISTS idx_core_users_attributes_gin ON entitlements.entitlement_core_users USING gin(global_attributes);

-- Update trigger for enhanced users
CREATE TRIGGER tr_core_users_updated_at 
    BEFORE UPDATE ON entitlements.entitlement_core_users 
    FOR EACH ROW EXECUTE FUNCTION entitlements.update_updated_at_column();

-- =====================================================================
-- PART 3: ADDITIONAL AUTHORIZATION TABLES
-- =====================================================================

-- ---------------------------------------------------------------------
-- User Domain Roles Table (User role assignments)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS entitlements.entitlement_user_domain_roles (
    user_role_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    assigned_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ,
    is_active BOOLEAN DEFAULT true,
    assigned_by UUID,
    assignment_metadata JSONB DEFAULT '{}',
    
    -- Foreign key constraints
    CONSTRAINT fk_user_domain_roles_user FOREIGN KEY (user_id) 
        REFERENCES entitlements.entitlement_core_users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_domain_roles_role FOREIGN KEY (role_id) 
        REFERENCES entitlements.entitlement_domain_roles(role_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_domain_roles_assigner FOREIGN KEY (assigned_by) 
        REFERENCES entitlements.entitlement_core_users(user_id),
    
    -- Unique constraint
    CONSTRAINT uk_user_domain_role UNIQUE(user_id, role_id)
);

-- Indexes for user domain roles
CREATE INDEX IF NOT EXISTS idx_user_domain_roles_user ON entitlements.entitlement_user_domain_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_domain_roles_role ON entitlements.entitlement_user_domain_roles(role_id);
CREATE INDEX IF NOT EXISTS idx_user_domain_roles_active ON entitlements.entitlement_user_domain_roles(is_active);
CREATE INDEX IF NOT EXISTS idx_user_domain_roles_expires ON entitlements.entitlement_user_domain_roles(expires_at);

-- ---------------------------------------------------------------------
-- Resource Permissions Table (ABAC direct permissions)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS entitlements.resource_permissions (
    resource_permission_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    resource_type VARCHAR(255) NOT NULL,
    resource_id VARCHAR(255) NOT NULL,
    allowed_actions TEXT[] NOT NULL,
    conditions JSONB DEFAULT '{}',
    granted_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ,
    is_active BOOLEAN DEFAULT true,
    granted_by UUID,
    
    -- Foreign key constraints
    CONSTRAINT fk_resource_permissions_user FOREIGN KEY (user_id) 
        REFERENCES entitlements.entitlement_core_users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_resource_permissions_granter FOREIGN KEY (granted_by) 
        REFERENCES entitlements.entitlement_core_users(user_id)
);

-- Indexes for resource permissions (ABAC)
CREATE INDEX IF NOT EXISTS idx_resource_permissions_user ON entitlements.resource_permissions(user_id);
CREATE INDEX IF NOT EXISTS idx_resource_permissions_resource ON entitlements.resource_permissions(resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_resource_permissions_active ON entitlements.resource_permissions(is_active);
CREATE INDEX IF NOT EXISTS idx_resource_permissions_expires ON entitlements.resource_permissions(expires_at);
CREATE INDEX IF NOT EXISTS idx_resource_permissions_conditions_gin ON entitlements.resource_permissions USING gin(conditions);

-- ---------------------------------------------------------------------
-- Audit Logs Table (Comprehensive audit trail)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS entitlements.entitlement_audit_logs (
    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_timestamp TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    event_type VARCHAR(100) NOT NULL,
    user_id UUID,
    resource_type VARCHAR(255),
    resource_id VARCHAR(255),
    action VARCHAR(255),
    decision VARCHAR(20), -- ALLOW, DENY
    decision_reason TEXT,
    request_metadata JSONB DEFAULT '{}',
    response_metadata JSONB DEFAULT '{}',
    session_id VARCHAR(255),
    ip_address INET,
    user_agent TEXT,
    
    -- Foreign key constraint
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) 
        REFERENCES entitlements.entitlement_core_users(user_id) ON DELETE SET NULL
);

-- Indexes for audit logs (optimized for reporting)
CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON entitlements.entitlement_audit_logs(event_timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user ON entitlements.entitlement_audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_event_type ON entitlements.entitlement_audit_logs(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_logs_resource ON entitlements.entitlement_audit_logs(resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_decision ON entitlements.entitlement_audit_logs(decision);
CREATE INDEX IF NOT EXISTS idx_audit_logs_session ON entitlements.entitlement_audit_logs(session_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_request_gin ON entitlements.entitlement_audit_logs USING gin(request_metadata);

-- Partition audit logs by month for better performance
CREATE TABLE IF NOT EXISTS entitlements.entitlement_audit_logs_y2025m08 
PARTITION OF entitlements.entitlement_audit_logs 
FOR VALUES FROM ('2025-08-01') TO ('2025-09-01');

CREATE TABLE IF NOT EXISTS entitlements.entitlement_audit_logs_y2025m09 
PARTITION OF entitlements.entitlement_audit_logs 
FOR VALUES FROM ('2025-09-01') TO ('2025-10-01');

-- =====================================================================
-- PART 4: PERFORMANCE OPTIMIZATION
-- =====================================================================

-- Additional composite indexes for complex queries
CREATE INDEX IF NOT EXISTS idx_users_active_username ON entitlements.users(is_active, username) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_user_roles_active_user ON entitlements.user_business_app_roles(user_id, is_active) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_departments_active_name ON entitlements.departments(is_active, department_name) WHERE is_active = true;

-- Partial indexes for active records only
CREATE INDEX IF NOT EXISTS idx_business_apps_active_only ON entitlements.business_applications(business_app_name) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_business_app_roles_active_only ON entitlements.business_app_roles(business_app_id, role_name) WHERE is_active = true;

-- =====================================================================
-- PART 5: VIEWS FOR COMMON QUERIES
-- =====================================================================

-- User permissions view (combines RBAC and ABAC)
CREATE OR REPLACE VIEW entitlements.v_user_permissions AS
SELECT 
    u.id as user_id,
    u.username,
    ba.business_app_name,
    bar.role_name,
    bar.role_display_name,
    d.department_name,
    d.department_code,
    bar.metadata as role_metadata,
    ubar.is_active as role_active,
    ud.is_active as dept_active
FROM entitlements.users u
LEFT JOIN entitlements.user_business_app_roles ubar ON u.id = ubar.user_id
LEFT JOIN entitlements.business_app_roles bar ON ubar.business_app_role_id = bar.id
LEFT JOIN entitlements.business_applications ba ON bar.business_app_id = ba.id
LEFT JOIN entitlements.user_departments ud ON u.id = ud.user_id
LEFT JOIN entitlements.departments d ON ud.department_id = d.id
WHERE u.is_active = true;

-- Active user summary view
CREATE OR REPLACE VIEW entitlements.v_active_user_summary AS
SELECT 
    u.id,
    u.username,
    u.email,
    u.first_name || ' ' || u.last_name as full_name,
    COUNT(DISTINCT ubar.business_app_role_id) as role_count,
    COUNT(DISTINCT ud.department_id) as department_count,
    ARRAY_AGG(DISTINCT d.department_code) FILTER (WHERE d.department_code IS NOT NULL) as department_codes,
    ARRAY_AGG(DISTINCT bar.role_name) FILTER (WHERE bar.role_name IS NOT NULL) as role_names,
    u.created_at,
    u.updated_at
FROM entitlements.users u
LEFT JOIN entitlements.user_business_app_roles ubar ON u.id = ubar.user_id AND ubar.is_active = true
LEFT JOIN entitlements.business_app_roles bar ON ubar.business_app_role_id = bar.id AND bar.is_active = true
LEFT JOIN entitlements.user_departments ud ON u.id = ud.user_id AND ud.is_active = true
LEFT JOIN entitlements.departments d ON ud.department_id = d.id AND d.is_active = true
WHERE u.is_active = true
GROUP BY u.id, u.username, u.email, u.first_name, u.last_name, u.created_at, u.updated_at;

-- =====================================================================
-- PART 6: SECURITY AND MAINTENANCE
-- =====================================================================

-- Enable row level security on sensitive tables
ALTER TABLE entitlements.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE entitlements.entitlement_audit_logs ENABLE ROW LEVEL SECURITY;

-- Create RLS policies (example - adjust based on requirements)
CREATE POLICY user_self_access ON entitlements.users 
    FOR ALL TO entitlement_user 
    USING (id = current_setting('app.current_user_id', true));

-- Grant appropriate permissions (adjust based on requirements)
GRANT SELECT, INSERT, UPDATE ON entitlements.users TO entitlement_user;
GRANT SELECT, INSERT, UPDATE ON entitlements.departments TO entitlement_user;
GRANT SELECT, INSERT, UPDATE ON entitlements.business_applications TO entitlement_user;
GRANT SELECT, INSERT, UPDATE ON entitlements.business_app_roles TO entitlement_user;
GRANT SELECT, INSERT, UPDATE ON entitlements.user_departments TO entitlement_user;
GRANT SELECT, INSERT, UPDATE ON entitlements.user_business_app_roles TO entitlement_user;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA entitlements TO entitlement_user;

-- =====================================================================
-- VERIFICATION QUERIES
-- =====================================================================

-- Verify table creation
SELECT 
    schemaname, 
    tablename, 
    tableowner 
FROM pg_tables 
WHERE schemaname = 'entitlements' 
ORDER BY tablename;

-- Verify indexes
SELECT 
    schemaname,
    tablename, 
    indexname,
    indexdef 
FROM pg_indexes 
WHERE schemaname = 'entitlements' 
ORDER BY tablename, indexname;

-- Verify constraints
SELECT 
    conname as constraint_name,
    contype as constraint_type,
    conrelid::regclass as table_name
FROM pg_constraint 
WHERE connamespace = 'entitlements'::regnamespace
ORDER BY conrelid::regclass, conname;

COMMENT ON SCHEMA entitlements IS 'NextGen Workflow Entitlement Service database schema containing core RBAC and hybrid RBAC/ABAC authorization tables';

-- =====================================================================
-- END OF SCRIPT
-- =====================================================================